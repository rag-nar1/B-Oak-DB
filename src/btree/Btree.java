package btree;


import java.util.ArrayDeque;
import java.util.Deque;

import bufferpool.BufferPool;
import bufferpool.ReadGuard;
import bufferpool.WriteGuard;
import globals.Globals;
import page.InternalNode;
import page.LeafNode;

public class Btree<KeyType extends Comparable<KeyType>, ValueType> {
    private final String fileName;
    private final Class<KeyType> keyType;
    private final Class<ValueType> valueType;
    private Comparable<KeyType> keyComparator;
    
    private long headerPageId;
    private BufferPool bufferPool;

    public Btree(String fileName,long headerPageId , BufferPool bufferPool, Class<KeyType> keyType, Class<ValueType> valueType) {
        this.fileName = fileName;
        this.keyType = keyType;
        this.valueType = valueType;
        this.headerPageId = headerPageId;
        this.bufferPool = bufferPool;

        // read the header page
        if (headerPageId != Globals.INVALID_PAGE_ID) {
            try {
                WriteGuard guard = bufferPool.getWriteGuard(fileName, headerPageId);
                BtreeHeader header = new BtreeHeader(guard.getData());
                header.setRootPageId(Globals.INVALID_PAGE_ID);
                header.setHeight((short) 0);
                header.writeHeader();
                guard.close();
            }
            catch (Exception e) {
                throw new RuntimeException("Error reading header page", e);
            }
        }
    }
    
    public boolean insert(KeyType key, ValueType value) throws Exception{
        // todo: add optimistic inserting
        // check if the B+ tree is empty
        Context ctx = new Context();
        WriteGuard guard = bufferPool.getWriteGuard(fileName, headerPageId);
        BtreeHeader header = new BtreeHeader(guard.getData());
        if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
            // create a new B+ tree
            long newRootPageId = bufferPool.allocateNewPage(fileName);
            WriteGuard newRootGuard = bufferPool.getWriteGuard(fileName, newRootPageId);
            LeafNode<KeyType, ValueType> newRoot = new LeafNode<>(keyType, valueType, newRootGuard.getDataMut());
            newRoot.setLeaf(true);
            newRoot.setPageId(newRootPageId);
            header.setRootPageId(newRootPageId);
            header.setHeight((short) 1);
            header.writeHeader();
            newRoot.insert(key, value);
            newRoot.writeHeader();
            newRootGuard.close();
            guard.close();
            return true;
        }
        ctx.setHeaderWriteGuard(guard);
        // get the root page id
        long rootPageId = header.getRootPageId();
        // get the root node
        long currentPageId = rootPageId;
        int lvl = 1;
        while(true) {
            WriteGuard currentGuard = bufferPool.getWriteGuard(fileName, currentPageId);
            if (lvl == header.getHeight()) { // we are at the leaf node level
                ctx.addWriteGuard(currentGuard);
                LeafNode<KeyType, ValueType> currentNode = new LeafNode<>(keyType, valueType, currentGuard.getDataMut());
                if (currentNode.insert(key, value)) { // if there is space in the node insert and we are done
                    currentNode.writeHeader();
                    ctx.release();
                    return true;
                }
                break; // if there is no space in the node we need to split
            }
            // if we are not at the leaf node level, we need to find the child node
            InternalNode<KeyType> currentNode = new InternalNode<>(keyType, currentGuard.getDataMut());
            // relase the locks over the above nodes since we are not going to split farther than this
            if (currentNode.getKeysN() < currentNode.getMaxKeysN()) { 
                ctx.release();
            }
            ctx.addWriteGuard(currentGuard);
            long childPageId = currentNode.getChildForKey(key);
            currentPageId = childPageId;
            lvl ++;
        }

        // we are at the leaf node level and we need to split the node and propagate the split up
        // get the leaf node
        WriteGuard currentGuard = ctx.peekFrontWrite();
        LeafNode<KeyType, ValueType> currentNode = new LeafNode<>(keyType, valueType, currentGuard.getDataMut());
        LeafNode<KeyType, ValueType> newNode = currentNode.split(bufferPool, fileName);
        if (newNode == null) {
            ctx.release();
            return false; // the node was not split
        }
        // check if the split node was the root
        if (currentNode.getPageId() == header.getRootPageId()) {
            // create a new root node
            long newRootPageId = bufferPool.allocateNewPage(fileName);
            WriteGuard newRootGuard = bufferPool.getWriteGuard(fileName, newRootPageId);
            InternalNode<KeyType> newRoot = new InternalNode<>(keyType, newRootGuard.getDataMut());
            newRoot.setLeaf(false);
            newRoot.setPageId(newRootPageId);
            // set the two child nodes
            newRoot.setValue(0, currentNode.getPageId());
            newRoot.setValue(1, newNode.getPageId());
            newRoot.setKey(1, currentNode.getKey(currentNode.getKeysN() - 1));
            newRoot.setKeysN((short) 2);
            newRoot.writeHeader();
            
            // update the header
            header.setRootPageId(newRootPageId);
            header.setHeight((short) (header.getHeight() + 1));
            header.writeHeader();
            
            newRootGuard.close();
            ctx.release();
            return true;
        }

        // we update the parent node to point to the new node rather than the old node
        // get the parent node
        WriteGuard parentGuard = ctx.peekBackWrite();
        InternalNode<KeyType> parentNode = new InternalNode<>(keyType, parentGuard.getDataMut());
        int index = parentNode.getKeyIdx(key);
        // update the child node
        parentNode.setValue(index - 1, newNode.getPageId());

        key = currentNode.getKey(currentNode.getKeysN() - 1);
        long propagatePageId = currentNode.getPageId();
        // we need to propagate the split up
        while(!ctx.writeGuardIsEmpty()) {
            WriteGuard currentInternalGuard = ctx.popBackWrite();
            InternalNode<KeyType> current = new InternalNode<>(keyType, currentInternalGuard.getDataMut());
            // check if the parent node is full
            if (current.getKeysN() < current.getMaxKeysN()) {
                // insert the new key and child node
                current.insert(key, propagatePageId);
                current.writeHeader();
                currentInternalGuard.close();
                continue;
            }
            // if the parent node is full we need to split it
            // create a new node
            InternalNode<KeyType> newInternalNode = current.split(bufferPool, fileName);
            if (newInternalNode == null) {
                currentInternalGuard.close();
                break; // the node was not split
            }
            // check if the split node was the root
            if (current.getPageId() == header.getRootPageId()) {
                // create a new root node
                long newRootPageId = bufferPool.allocateNewPage(fileName);
                WriteGuard newRootGuard = bufferPool.getWriteGuard(fileName, newRootPageId);
                InternalNode<KeyType> newRoot = new InternalNode<>(keyType, newRootGuard.getDataMut());
                newRoot.setLeaf(false);
                newRoot.setPageId(newRootPageId);
                // set the two child nodes
                newRoot.setValue(0, current.getPageId());
                newRoot.setValue(1, newInternalNode.getPageId());
                newRoot.setKey(1, current.getKey(current.getKeysN() - 1));
                newRoot.setKeysN((short) 2);
                newRoot.writeHeader();
                
                // update the header
                header.setRootPageId(newRootPageId);
                header.setHeight((short) (header.getHeight() + 1));
                header.writeHeader();
                currentInternalGuard.close();
                break;
            }

            // update the parent node to point to the new node rather than the old node
            // get the parent node
            parentGuard = ctx.peekBackWrite();
            InternalNode<KeyType> parent = new InternalNode<>(keyType, parentGuard.getDataMut());
            index = parent.getKeyIdx(key);
            parent.setValue(index, newInternalNode.getPageId());

            key = current.getKey(current.getKeysN() - 1);
            propagatePageId = current.getPageId();
        }

        // release the locks
        ctx.release();
        return true;
    }
    
    // getters and setters
    public boolean isEmpty() {
        try {
            ReadGuard guard = bufferPool.getReadGuard(fileName, headerPageId);
            BtreeHeader header = new BtreeHeader(guard.getData());
            boolean isEmpty = header.isEmpty();
            guard.close();
            return isEmpty;
        } catch (Exception e) {
            throw new RuntimeException("Error checking if B+ tree is empty", e);
        }
    }

    public void setEmpty() {
        try {
            WriteGuard guard = bufferPool.getWriteGuard(fileName, headerPageId);
            BtreeHeader header = new BtreeHeader(guard.getData());
            header.setRootPageId(Globals.INVALID_PAGE_ID);
            header.setHeight((short) 0);
            header.writeHeader();
            guard.close();
        } catch (Exception e) {
            throw new RuntimeException("Error setting B+ tree to empty", e);
        }
    }

    public long getRootPageId() {
        try {
            ReadGuard guard = bufferPool.getReadGuard(fileName, headerPageId);
            BtreeHeader header = new BtreeHeader(guard.getData());
            long rootPageId = header.getRootPageId();
            guard.close();
            return rootPageId;
        } catch (Exception e) {
            throw new RuntimeException("Error getting root page ID", e);
        }
    }
   
    public String getFileName() {
        return fileName;
    }

    public long getHeaderPageId() {
        return headerPageId;
    }

    public void setHeaderPageId(long headerPageId) {
        this.headerPageId = headerPageId;
    }

    public Class<KeyType> getKeyType() {
        return keyType;
    }

    public Class<ValueType> getValueType() {
        return valueType;
    }

    private class Context {
        private WriteGuard headeWriteGuard;
        private Deque<WriteGuard> writeGuards;
        private Deque<ReadGuard> readGuards;

        public Context() {
            writeGuards = new ArrayDeque<>();
            readGuards = new ArrayDeque<>();
        }

        public boolean writeGuardIsEmpty() {
            return writeGuards.isEmpty();
        }

        public void setHeaderWriteGuard(WriteGuard guard) {
            this.headeWriteGuard = guard;
        }

        public WriteGuard getHeaderWriteGuard() {
            return headeWriteGuard;
        }

        public void addWriteGuard(WriteGuard guard) {
            writeGuards.push(guard);
        }

        public void dropHeaderWriteGuard() {
            if (headeWriteGuard != null) {
                headeWriteGuard.close();
                headeWriteGuard = null;
            }
        }

        public void addReadGuard(ReadGuard guard) {
            readGuards.push(guard);
        }

        public WriteGuard peekFrontWrite() {
            return writeGuards.peekFirst();
        }

        public ReadGuard peekFrontRead() {
            return readGuards.peekFirst();
        }

        public WriteGuard peekBackWrite() {
            return writeGuards.peekLast();
        }

        public ReadGuard peekBackRead() {
            return readGuards.peekLast();
        }

        public WriteGuard popFrontWrite() {
            return writeGuards.removeFirst();
        }

        public ReadGuard popFrontRead() {
            return readGuards.removeFirst();
        }

        public WriteGuard popBackWrite() {
            return writeGuards.removeLast();
        }   

        public ReadGuard popBackRead() {
            return readGuards.removeLast();
        }

        public void release() {
            dropHeaderWriteGuard();
            while (!writeGuards.isEmpty()) {
                WriteGuard guard = writeGuards.pop();
                guard.close();
            }
            while (!readGuards.isEmpty()) {
                ReadGuard guard = readGuards.pop();
                guard.close();
            }
        }


    }
}
