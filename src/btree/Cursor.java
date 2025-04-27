package btree;

import bufferpool.BufferPool;
import bufferpool.ReadGuard;
import globals.Globals;
import page.LeafNode;

public class Cursor<KeyType extends Comparable<KeyType>, ValueType> {
    private ReadGuard guard;
    private BufferPool bufferpool;
    private Btree<KeyType, ValueType> btree;
    private LeafNode<KeyType, ValueType> node;
    private int index;

    public Cursor(Btree<KeyType, ValueType> btree, ReadGuard guard, LeafNode<KeyType, ValueType> node) {
        this.btree = btree;
        this.bufferpool = btree.getBufferPool();
        this.guard = guard;
        this.node = node;
    }

    public Pair<KeyType, ValueType> get() {
        Pair<KeyType, ValueType> curr = new Pair<KeyType,ValueType>(node.getKey(index), node.getValue(index));
        return curr;
    }

    public void next() throws Exception {
        if (isEnd()) {
            throw new Exception("current curser is the end of the b+tree");
        }
        index++;
        if (index < node.getKeysN()) {
            return;
        }
        // go to the next leaf
        long nextPageId = node.getNextLeafNode();
        if (nextPageId == Globals.INVALID_PAGE_ID) {
            end();
            return;
        }
        while(true) {
            ReadGuard nextGuard = bufferpool.getReadGuard(btree.getFileName(), nextPageId);
            if (nextGuard == null) {
                Thread.sleep(10);
                continue;
            }
            LeafNode<KeyType, ValueType> nextNode = new LeafNode<>(btree.getKeyType(), btree.getValueType(),
                    nextGuard.getData());

            node = nextNode;
            guard.close();
            guard = nextGuard;
            index = 0;
            break;
        }
    }

    public void end() {
        index = -1;
    }

    public boolean isEnd() {
        return index == -1;
    } 

    public class Pair<U, V> {
        public final U first;
        public final V second;

        public Pair(U first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
