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
    
    private long headerPageId;
    private BufferPool bufferPool;

    public Btree(Class<KeyType> keyType, Class<ValueType> valueType, String fileName,long headerPageId , BufferPool bufferPool) {
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
                guard.close();
            }
            catch (Exception e) {
                throw new RuntimeException("Error reading header page", e);
            }
        } else {
            // create a new header page
            try {
                this.headerPageId = bufferPool.allocateNewPage(fileName);
                WriteGuard guard = bufferPool.getWriteGuard(fileName, this.headerPageId);
                BtreeHeader header = new BtreeHeader(guard.getDataMut());
                header.setRootPageId(Globals.INVALID_PAGE_ID);
                header.setHeight((short) 0);
                guard.close();
            } catch (Exception e) {
                throw new RuntimeException("Error creating header page", e);
            }
        }
    }

    public ValueType get(KeyType key) throws Exception {
        Context ctx = new Context();
        ReadGuard guard = bufferPool.getReadGuard(fileName, headerPageId);
        BtreeHeader header = new BtreeHeader(guard.getData());
        if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
            guard.close();
            return null; // the tree is empty
        }

        long rootPageId = header.getRootPageId();
        long currentPageId = rootPageId;
        int lvl = 1;
        ValueType value = null;
        while (true) {
            ReadGuard currentGuard = bufferPool.getReadGuard(fileName, currentPageId);
            if (lvl == header.getHeight()) { // we are at the leaf node level
                ctx.addReadGuard(currentGuard);
                LeafNode<KeyType, ValueType> currentNode = new LeafNode<>(keyType, valueType, currentGuard.getData());
                value = currentNode.get(key);
                ctx.release();
                break; // we are done
            }
            // if we are not at the leaf node level, we need to find the child node
            InternalNode<KeyType> currentNode = new InternalNode<>(keyType, currentGuard.getData());
            ctx.addReadGuard(currentGuard);
            long childPageId = currentNode.getChildForKey(key);
            currentPageId = childPageId;
            lvl++;
        }
        
        // release the header guard
        guard.close();
        return value;
    }
    
    public boolean insert(KeyType key, ValueType value) throws Exception{
        // todo: add optimistic inserting
        // check if the B+ tree is empty
        Context ctx = new Context();
        WriteGuard guard = bufferPool.getWriteGuard(fileName, headerPageId);
        BtreeHeader header = new BtreeHeader(guard.getDataMut());
        if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
            // create a new B+ tree
            long newRootPageId = bufferPool.allocateNewPage(fileName);
            WriteGuard newRootGuard = bufferPool.getWriteGuard(fileName, newRootPageId);
            LeafNode<KeyType, ValueType> newRoot = new LeafNode<>(keyType, valueType, newRootGuard.getDataMut());
            newRoot.setLeaf(true);
            newRoot.setNextLeafNode(Globals.INVALID_PAGE_ID);
            newRoot.setPageId(newRootPageId);

            header.setRootPageId(newRootPageId);
            header.setHeight((short) 1);

            newRoot.insert(key, value);
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
        WriteGuard currentGuard = ctx.popFrontWrite();
        LeafNode<KeyType, ValueType> currentNode = new LeafNode<>(keyType, valueType, currentGuard.getDataMut());
        WriteGuard newNodeguard = currentNode.split(bufferPool, fileName);
        if (newNodeguard == null) {
            ctx.release();
            return false; // the node was not split
        }
        LeafNode<KeyType, ValueType> newNode = new LeafNode<>(keyType, valueType, newNodeguard.getDataMut());
        // insert the key into the correct node
        if (key.compareTo(currentNode.getKey(currentNode.getKeysN() - 1)) <= 0) {
            currentNode.insert(key, value);
        } else {
            newNode.insert(key, value);
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
            
            // update the header
            header.setRootPageId(newRootPageId);
            header.setHeight((short) (header.getHeight() + 1));
            
            newRootGuard.close();
            newNodeguard.close();
            currentGuard.close();
            ctx.release();
            return true;
        }

        // we update the parent node to point to the new node rather than the old node
        // get the parent node
        WriteGuard parentGuard = ctx.peekFrontWrite();
        InternalNode<KeyType> parentNode = new InternalNode<>(keyType, parentGuard.getDataMut());
        int index = parentNode.getKeyIdx(key);
        // update the child node
        parentNode.setValue(index - 1, newNode.getPageId());

        key = currentNode.getKey(currentNode.getKeysN() - 1);
        long propagatePageId = currentNode.getPageId();
        currentGuard.close();
        newNodeguard.close();
        // we need to propagate the split up
        while(!ctx.writeGuardIsEmpty()) {
            WriteGuard currentInternalGuard = ctx.popFrontWrite();
            InternalNode<KeyType> current = new InternalNode<>(keyType, currentInternalGuard.getDataMut());
            // check if the parent node is full
            if (current.getKeysN() < current.getMaxKeysN()) {
                // insert the new key and child node
                current.insert(key, propagatePageId);
                currentInternalGuard.close();
                break;
            }
            // if the parent node is full we need to split it
            // create a new node
            WriteGuard newInternalNodeGuard = current.split(bufferPool, fileName);
            if (newInternalNodeGuard == null) {
                currentInternalGuard.close();
                break; // the node was not split
            }
            InternalNode<KeyType> newInternalNode = new InternalNode<>(keyType, newInternalNodeGuard.getDataMut());
            // insert the key value
            if (key.compareTo(current.getKey(current.getKeysN() - 1)) <= 0) {
                current.insert(key, propagatePageId);
            } else {
                newInternalNode.insert(key, propagatePageId);
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
                newRoot.setKey(1, newInternalNode.getKey(0));
                newRoot.setKeysN((short) 2);
                
                // update the header
                header.setRootPageId(newRootPageId);
                header.setHeight((short) (header.getHeight() + 1));
                currentInternalGuard.close();
                newInternalNodeGuard.close();
                break;
            }

            // update the parent node to point to the new node rather than the old node
            // get the parent node
            parentGuard = ctx.peekFrontWrite();
            InternalNode<KeyType> parent = new InternalNode<>(keyType, parentGuard.getDataMut());
            index = parent.getKeyIdx(key);
            parent.setValue(index - 1, newInternalNode.getPageId());

            key = newInternalNode.getKey(0);
            propagatePageId = current.getPageId();
            currentInternalGuard.close();
            newInternalNodeGuard.close();
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
