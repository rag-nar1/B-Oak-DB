package btree;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import bufferpool.BufferPool;
import bufferpool.ReadGuard;
import bufferpool.WriteGuard;
import globals.Globals;
import page.InternalNode;
import page.LeafNode;
import types.Compositekey;
import types.Template;

public class Btree implements Index {
    private final String fileName;
    private final Template keyType;
    private final Template valueType;

    private long headerPageId;
    private BufferPool bufferPool;

    public Btree(Template keyType, Template valueType, String fileName, long headerPageId,
            BufferPool bufferPool) {
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
            } catch (Exception e) {
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

    public Compositekey get(Compositekey key) throws Exception {
        out: while (true) {
            Context ctx = new Context();
            ReadGuard guard = bufferPool.getReadGuard(fileName, headerPageId);
            if (guard == null) {
                Thread.sleep(10);
                continue out;
            }
            BtreeHeader header = new BtreeHeader(guard.getData());
            if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
                guard.close();
                return null; // the tree is empty
            }
            ctx.setHeaderReadGuard(guard);
            long rootPageId = header.getRootPageId();
            long currentPageId = rootPageId;
            int lvl = 1;
            while (true) {
                ReadGuard currentGuard = bufferPool.getReadGuard(fileName, currentPageId);
                if (currentGuard == null) {
                    ctx.release();
                    Thread.sleep(10);
                    continue out;
                }

                ctx.addReadGuard(currentGuard);
                if (lvl == header.getHeight()) { // we are at the leaf node level
                    LeafNode currentNode = new LeafNode(keyType, valueType,
                            currentGuard.getData());
                    Compositekey value = currentNode.get(key);
                    ctx.release();
                    return value;
                }
                // if we are not at the leaf node level, we need to find the child node
                InternalNode currentNode = new InternalNode(keyType, currentGuard.getData());
                Compositekey childPageId = currentNode.getChildForKey(key);
                currentPageId = childPageId.<Long>getVal(0);
                lvl++;
                ctx.release();
            }
        }
    }

    public boolean insert(Compositekey key, Compositekey value) throws Exception {
        out: while (true) {
            int tryInsertOpt = optimisticInsert(key, value);
            if (tryInsertOpt != 0) {
                return (tryInsertOpt == 1);
            }
            // check if the B+ tree is empty
            Context ctx = new Context();
            WriteGuard guard = bufferPool.getWriteGuard(fileName, headerPageId);
            if (guard == null) {
                Thread.sleep(10);
                continue;
            }
            BtreeHeader header = new BtreeHeader(guard.getDataMut());
            if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
                // create a new B+ tree
                long newRootPageId = bufferPool.allocateNewPage(fileName);
                WriteGuard newRootGuard = bufferPool.getWriteGuard(fileName, newRootPageId);
                if (newRootGuard == null) {
                    guard.close();
                    Thread.sleep(10);
                    continue;
                }
                LeafNode newRoot = new LeafNode(keyType, valueType, newRootGuard.getDataMut());
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
            while (true) {
                WriteGuard currentGuard = bufferPool.getWriteGuard(fileName, currentPageId);
                if (currentGuard == null) {
                    ctx.release();
                    Thread.sleep(10);
                    continue out;
                }
                if (lvl == header.getHeight()) { // we are at the leaf node level
                    ctx.addWriteGuard(currentGuard);
                    LeafNode currentNode = new LeafNode(keyType, valueType,
                            currentGuard.getDataMut());
                    if (currentNode.insert(key, value) != 0) { // if there is space in the node insert and we are done
                        ctx.release();
                        return true;
                    }
                    break; // if there is no space in the node we need to split
                }
                // if we are not at the leaf node level, we need to find the child node
                InternalNode currentNode = new InternalNode(keyType, currentGuard.getDataMut());
                // relase the locks over the above nodes since we are not going to split farther
                // than this
                if (currentNode.getKeysN() < currentNode.getMaxKeysN()) {
                    ctx.release();
                }
                ctx.addWriteGuard(currentGuard);
                Compositekey childPageId = currentNode.getChildForKey(key);
                currentPageId = childPageId.<Long>getVal(0);
                lvl++;
            }

            // we are at the leaf node level and we need to split the node and propagate the
            // split up
            // get the leaf node
            WriteGuard currentGuard = ctx.popFrontWrite();
            LeafNode currentNode = new LeafNode(keyType, valueType, currentGuard.getDataMut());
            WriteGuard newNodeguard = currentNode.split(bufferPool, fileName);
            if (newNodeguard == null) {
                currentGuard.close();
                ctx.release();
                Thread.sleep(10);
                continue;
            }
            LeafNode newNode = new LeafNode(keyType, valueType, newNodeguard.getDataMut());
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
                InternalNode newRoot = new InternalNode(keyType, newRootGuard.getDataMut());
                newRoot.setLeaf(false);
                newRoot.setPageId(newRootPageId);
                // set the two child nodes
                newRoot.setValue(0, makeCompositekeyValue(currentNode.getPageId()));
                newRoot.setValue(1, makeCompositekeyValue(newNode.getPageId()));
                newRoot.setKey(1, currentNode.getKey(currentNode.getKeysN() - 1));
                newRoot.setCompositekeyN((short) 2);

                // update the header
                header.setRootPageId(newRootPageId);
                header.setHeight((short) (header.getHeight() + 1));

                newNodeguard.close();
                currentGuard.close();
                newRootGuard.close();
                ctx.release();
                return true;
            }

            // we update the parent node to point to the new node rather than the old node
            // get the parent node
            WriteGuard parentGuard = ctx.peekFrontWrite();
            InternalNode parentNode = new InternalNode(keyType, parentGuard.getDataMut());
            int index = parentNode.getKeyIdx(key);
            // update the child node
            parentNode.setValue(index - 1, makeCompositekeyValue(newNode.getPageId()));

            key = currentNode.getKey(currentNode.getKeysN() - 1);
            long propagatePageId = currentNode.getPageId();
            newNodeguard.close();
            currentGuard.close();
            // we need to propagate the split up
            while (!ctx.writeGuardIsEmpty()) {
                WriteGuard currentInternalGuard = ctx.popFrontWrite();
                InternalNode current = new InternalNode(keyType, currentInternalGuard.getDataMut());
                // check if the parent node is full
                if (current.getKeysN() < current.getMaxKeysN()) {
                    // insert the new key and child node
                    current.insert(key, makeCompositekeyValue(propagatePageId));
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
                InternalNode newInternalNode = new InternalNode(keyType, newInternalNodeGuard.getDataMut());
                // insert the key value
                if (key.compareTo(current.getKey(current.getKeysN() - 1)) <= 0) {
                    current.insert(key, makeCompositekeyValue(propagatePageId));
                } else {
                    newInternalNode.insert(key, makeCompositekeyValue(propagatePageId));
                }
                // check if the split node was the root
                if (current.getPageId() == header.getRootPageId()) {
                    // create a new root node

                    long newRootPageId = bufferPool.allocateNewPage(fileName);

                    WriteGuard newRootGuard = bufferPool.getWriteGuard(fileName, newRootPageId);

                    InternalNode newRoot = new InternalNode(keyType, newRootGuard.getDataMut());

                    newRoot.setLeaf(false);
                    newRoot.setPageId(newRootPageId);

                    // set the two child nodes
                    newRoot.setValue(0, makeCompositekeyValue(current.getPageId()));
                    newRoot.setValue(1, makeCompositekeyValue(newInternalNode.getPageId()));
                    newRoot.setKey(1, newInternalNode.getKey(0));
                    newRoot.setCompositekeyN((short) 2);

                    // update the header
                    header.setRootPageId(newRootPageId);
                    header.setHeight((short) (header.getHeight() + 1));
                    currentInternalGuard.close();
                    newInternalNodeGuard.close();
                    newRootGuard.close();
                    break;
                }

                // update the parent node to point to the new node rather than the old node
                // get the parent node
                parentGuard = ctx.peekFrontWrite();
                InternalNode parent = new InternalNode(keyType, parentGuard.getDataMut());
                index = parent.getKeyIdx(key);
                parent.setValue(index - 1, makeCompositekeyValue(newInternalNode.getPageId()));

                key = newInternalNode.getKey(0);
                propagatePageId = current.getPageId();
                currentInternalGuard.close();
                newInternalNodeGuard.close();
            }

            // release the locks
            ctx.release();
            return true;
        }
    }

    private int optimisticInsert(Compositekey key, Compositekey value) throws Exception {
        Context ctx = new Context();
        ReadGuard guard = bufferPool.getReadGuard(fileName, headerPageId);
        if (guard == null) {
            return 0;
        }
        BtreeHeader header = new BtreeHeader(guard.getData());
        if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
            guard.close();
            return 0; // the tree is empty
        }

        long rootPageId = header.getRootPageId();
        long currentPageId = rootPageId;
        int lvl = 1;
        ctx.addReadGuard(guard);
        while (true) {
            if (lvl == header.getHeight()) { // we are at the leaf node level
                WriteGuard currentGuard = bufferPool.getWriteGuard(fileName, currentPageId);
                if (currentGuard == null) {
                    ctx.release();
                    return 0;
                }
                LeafNode currentNode = new LeafNode(keyType, valueType,
                        currentGuard.getDataMut());
                int tryInsert = currentNode.insert(key, value);
                currentGuard.close();
                ctx.release();
                return tryInsert;
            }
            ReadGuard currentGuard = bufferPool.getReadGuard(fileName, currentPageId);
            if (currentGuard == null) {
                ctx.release();
                return 0;
            }
            ctx.addReadGuard(currentGuard);
            // if we are not at the leaf node level, we need to find the child node
            InternalNode currentNode = new InternalNode(keyType, currentGuard.getData());
            Compositekey childPageId = currentNode.getChildForKey(key);
            currentPageId = childPageId.<Long>getVal(0);
            lvl++;
        }
    }

    private Compositekey makeCompositekeyForInternal(long pageId) {
        Compositekey key = new Compositekey(valueType);
        key.set(0, pageId, Long.class);
        return key;
    }

    private void redistribute(Context ctx) throws Exception{
        // should have 2 nodes leaf and parent
        WriteGuard currGuard = ctx.popBackWrite(); 
        WriteGuard parentGuard = ctx.peekBackWrite();
        LeafNode currNode = new LeafNode(keyType, valueType, currGuard.getDataMut());
        InternalNode parenNode = new InternalNode(keyType, currGuard.getDataMut());
    
        // try get guaed on the left child 
        int index = parenNode.getKeyIdx(currNode.getKey(currNode.getKeysN() - 1));
        if (parenNode.getValue(index - 1).compareTo(makeCompositekeyForInternal(currNode.getPageId())) != 0) {
            // there is a left child
            ByteBuffer buf = ByteBuffer.wrap(parenNode.getValue(index - 1).get(0).get());
            WriteGuard siblingGuard = bufferPool.getWriteGuard(fileName, buf.getLong());
            if (siblingGuard != null) { // try redestibute
                LeafNode siblingNode = new LeafNode(keyType, valueType, siblingGuard.getDataMut());
                if (siblingNode.getKeysN() < siblingNode.getMaxKeysN() - 1) {
                    // insert the smallest key value from current node into sibiling
                    siblingNode.insert(currNode.getKey(0), currNode.getValue(0));
                    // delete the smallest key value from current node
                    currNode.delete(0);
                    // update the parent key pointing to the sibiling node
                    parenNode.setKey(index, siblingNode.getKey(siblingNode.getKeysN() - 1));
                }
            }
        }

        // could not redistrebute
    }

    public boolean delete(Compositekey key) throws Exception {
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

    public Template getKeyType() {
        return keyType;
    }

    public Template getValueType() {
        return valueType;
    }

    public BufferPool getBufferPool() {
        return bufferPool;
    }

    private class Context {
        private WriteGuard headeWriteGuard;
        private ReadGuard headeReadGuard;
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

        public void setHeaderReadGuard(ReadGuard guard) {
            this.headeReadGuard = guard;
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

        public void dropHeaderReadGuard() {
            if (headeReadGuard != null) {
                headeReadGuard.close();
                headeReadGuard = null;
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
            dropHeaderReadGuard();
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

    // cursor

    public Cursor begin() throws Exception {
        out: while(true) {
            Context ctx = new Context();
            ReadGuard guard = bufferPool.getReadGuard(fileName, headerPageId);
            if (guard == null) {
                Thread.sleep(10);
                continue out;
            }
            BtreeHeader header = new BtreeHeader(guard.getData());
            if (header.getRootPageId() == Globals.INVALID_PAGE_ID) {
                guard.close();
                return null; // the tree is empty
            }

            ctx.setHeaderReadGuard(guard);
            long rootPageId = header.getRootPageId();
            long currentPageId = rootPageId;
            int lvl = 1;
            Cursor itr;
            while (true) {
                ReadGuard currentGuard = bufferPool.getReadGuard(fileName, currentPageId);
                if (currentGuard == null) {
                    ctx.release();
                    Thread.sleep(10);
                    continue out;
                }

                if (lvl == header.getHeight()) { // we are at the leaf node level
                    LeafNode node = new LeafNode(keyType, valueType,
                    currentGuard.getData());
                    itr = new Cursor(this, currentGuard, node);
                    break; // we are done
                }
                
                ctx.addReadGuard(currentGuard);
                // if we are not at the leaf node level, we need to find the child node
                InternalNode currentNode = new InternalNode(keyType, currentGuard.getData());
                Compositekey childPageId = currentNode.getValue(0);
                currentPageId = childPageId.<Long>getVal(0);
                lvl++;
                ctx.release();
            }
            ctx.release();
            return itr;
        }
    }

    // utils

    private Compositekey makeCompositekeyValue(long val) {
        Compositekey key = new Compositekey(valueType);
        ByteBuffer buf = ByteBuffer.wrap(new byte[Long.BYTES]);
        buf.putLong(val);
        key.set(0, buf.array());
        return key;
    }

}
