package page;

import java.io.Serializable;

/**
 * InternalNode class represents an internal node in a B+ tree.
 * It extends the TreeNode class and is used to store keys and child pointers.
 * The keys are used to navigate the tree, while the child pointers are pageIds
 * that point to the child nodes.
 |  null |key2   |key3   |...|keyN   |
 |pageId1|pageId2|pageId3|...|pageIdN|
    pageId_i points to the subtree where keys there sutisfy key_i < key <= key_(i+1)
    * The last pageId points to the subtree where keys are greater than keyN.
 */
public class InternalNode<KeyType extends Comparable<KeyType> & Serializable> extends TreeNodeHeader {
   
}
