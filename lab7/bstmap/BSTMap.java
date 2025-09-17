package bstmap;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

    private BTSNode root;

    @Override
    public void clear() {
        this.root = null;
    }

    @Override
    public boolean containsKey(K key) {
        return containsKey(root, key);
    }

    private boolean containsKey(BTSNode node, K key){
        if (node == null) {
            return false;
        }

        int tmp = key.compareTo(node.key);
        if (tmp == 0) {
            return true;
        } else {
            if (tmp > 0) {
                return containsKey(node.right, key);
            } else {
                return containsKey(node.left, key);
            }
        }
    }

    @Override
    public V get(K key) {
        return get(root, key);
    }

    private V get(BTSNode node, K key) {
        if (node == null) {
            return null;
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return get(node.left, key);
        } else if (cmp > 0) {
            return get(node.right, key);
        } else {
            return node.value;
        }
    }

    @Override
    public int size() {
        return size(root);
    }

    private int size(BTSNode node) {
        if (node == null) {
            return 0;
        }

        return 1 + size(node.left) + size(node.right);
    }

    @Override
    public void put(K key, V value) {
        root = put(root, key, value);
    }

    private BTSNode put(BTSNode node, K key, V value) {
        if (node == null) {
            return new BTSNode(key, value);
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = put(node.left, key, value);
        } else if (cmp > 0) {
            node.right = put(node.right, key, value);
        } else {
            node.value = value;
        }
        return node;
    }

    @Override
    public V remove(K key) {
        // 1. 首先找到要删除的节点
        BTSNode deleting = findNode(key);

        // 2.【修复】处理节点不存在的情况
        if (deleting == null) {
            return null; // 或者抛出异常
        }

        V value = deleting.value;
        removeNode(deleting); // 调用一个辅助函数来处理具体的删除逻辑
        return value;
    }

    public void printInOrder() {
        for (K key : this) {
            System.out.println(key);
        }
    }

    /**
     * 辅助函数，专门负责删除节点和重构树
     * @param deleting 要删除的节点 (确保不为 null)
     */
    // 一个专门处理双子节点删除的、非递归的、更健壮的逻辑
    private void removeTwoChildNode(BTSNode deleting) {
        // 1. 同时找到后继节点(successor)和它的父节点(successorParent)
        BTSNode successorParent = deleting;
        BTSNode successor = deleting.right;
        while (successor.left != null) {
            successorParent = successor;
            successor = successor.left;
        }

        // 2. 将后继节点的数据复制到正在被“删除”的节点
        deleting.key = successor.key;
        deleting.value = successor.value;

        // 3. 现在问题转化为删除后继节点，我们已经有它的父节点了
        //    后继节点只可能有右子节点
        if (successorParent.left == successor) {
            // 如果后继是其父的左孩子
            successorParent.left = successor.right;
        } else { // successorParent.right == successor (也即 successorParent == deleting)
            // 如果后继是其父的右孩子
            successorParent.right = successor.right;
        }
    }

    // 将这个新方法整合进你的主删除函数
    private void removeNode(BTSNode deleting) {
        // 情况一: 节点有两个孩子
        if (deleting.left != null && deleting.right != null) {
            removeTwoChildNode(deleting);
        }
        // 情况二: 节点有零个或一个孩子
        else {
            BTSNode child = (deleting.left != null) ? deleting.left : deleting.right;
            BTSNode parent = findNodeParent(deleting); // 在这里，你原来的 findNodeParent 没问题

            if (parent == null) {
                root = child; // 正在删除根节点
            } else if (parent.left == deleting) {
                parent.left = child;
            } else {
                parent.right = child;
            }
        }
    }

    // (您需要实现 findSuccessor 方法)
    private BTSNode findSuccessor(BTSNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    @Override
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        Set<K> resultkeySet = new HashSet<>();

        for (K key : this) {
            resultkeySet.add(key);
        }

        return resultkeySet;
    }

    @Override
    public Iterator<K> iterator() {
        return new BTSIterator(root);
    }

    //我试着实现一个前序遍历
    private class BTSIterator implements Iterator<K> {

        public BTSNode currNode;

        public BTSIterator(BTSNode node) {
            currNode = node;
        }

        /** currNode can be never null, so if it has the right
         * @return whether it has the next element
         */
        @Override
        public boolean hasNext() {
            return currNode != null;
        }

        @Override
        public K next() {
            if (hasNext()) {
                while (currNode != null){
                    if (currNode.left == null) {
                        BTSNode tmp = currNode;
                        currNode = currNode.right;
                        return tmp.key;
                    } else {
                        BTSNode predecessor = findTheRightMost(currNode.left, currNode);

                        if (predecessor.right == null) {
                            predecessor.right = currNode;
                            currNode = currNode.left;
                        } else if (predecessor.right == currNode) {
                            predecessor.right = null;
                            BTSNode tmp = currNode;
                            currNode = currNode.right;
                            return tmp.key;
                        }
                    }
                }
            }
            return null;
        }

        private BTSNode findTheRightMost(BTSNode root, BTSNode current) {
            BTSNode rightmostNode = root;
            // Stop if the right pointer is null OR if it's a thread pointing back to our current node
            while (rightmostNode.right != null && rightmostNode.right != current) {
                rightmostNode = rightmostNode.right;
            }
            return rightmostNode;
        }
    }

    private BTSNode findNode(K key) {
        BTSNode current = root; // 从根节点开始

        // 循环直到找到节点或者到达树的末端 (null)
        while (current != null) {
            int cmp = key.compareTo(current.key);

            if (cmp < 0) {
                // 目标 key 小于当前节点的 key，去左子树查找
                current = current.left;
            } else if (cmp > 0) {
                // 目标 key 大于当前节点的 key，去右子树查找
                current = current.right;
            } else {
                // cmp == 0，找到了匹配的节点
                return current;
            }
        }

        // 如果循环结束了 current 仍然是 null，说明没有找到
        return null;
    }

    private BTSNode findNodeParent(BTSNode node) {
        // 如果树为空或者要找的是根节点的父节点，直接返回 null
        if (root == null || root == node) {
            return null;
        }

        BTSNode current = root;
        while (current != null) {
            // 检查左子节点
            if (node.key.compareTo(current.key) < 0) {
                if (current.left == null) {
                    // 节点不存在
                    return null;
                } else if (current.left == node) {
                    // 找到了父节点
                    return current;
                }
                // 继续向左移动
                current = current.left;
            }
            // 检查右子节点
            else if (node.key.compareTo(current.key) > 0) {
                if (current.right == null) {
                    // 节点不存在
                    return null;
                } else if (current.right == node) {
                    // 找到了父节点
                    return current;
                }
                // 继续向右移动
                current = current.right;
            }
            // 如果 key 相等但节点对象不同，这表示树结构有问题或有重复值
            // 在这种情况下，无法确定父节点，可以返回 null
            else {
                return null;
            }
        }
        return null; // 循环结束也没找到
    }

    private BTSNode findPredecessor(BTSNode node) {
        BTSNode Predecessor = node;
        if (node == null) {
            return  null;
        }
        // Stop if the right pointer is null
        while (Predecessor.right != null) {
            Predecessor = Predecessor.right;
        }
        return Predecessor;
    }

    private class BTSNode{
        public K key;
        public V value;
        public BTSNode left;
        public BTSNode right;

        public BTSNode (K key, V value) {
            this.key = key;
            this.value = value;
            left = null;
            right = null;
        }
    }
}
