package edu.grinnell.csc207.compression;

import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.AbstractMap;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {
    private static final short EOF = 256;
    private final Node root;
    private final Map<Short,String> codes;

    private static class Node implements Comparable<Node> {
        short value;
        int frequency;
        Node left, right;

        Node(short v, int freq) {
            this.value = v;
            this.frequency = freq;
        }

        Node(Node l, Node r) {
            this.left = l;
            this.right = r;
            this.frequency = l.frequency + r.frequency;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.frequency, other.frequency);
        }
    }

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        freqs.put(EOF, freqs.getOrDefault(EOF, 0) + 1);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Short, Integer> e : freqs.entrySet()) {
            pq.add(new Node(e.getKey(), e.getValue()));
        }
        while (pq.size() > 1) {
            Node a = pq.poll();
            Node b = pq.poll();
            pq.add(new Node(a, b));
        }
        root = pq.poll();

        codes = new HashMap<>();
        Deque<AbstractMap.SimpleEntry<Node, String>> stack = new ArrayDeque<>();
        stack.push(new AbstractMap.SimpleEntry<>(root, ""));
        while (!stack.isEmpty()) {
            AbstractMap.SimpleEntry<Node, String> entry = stack.pop();
            Node node = entry.getKey();
            String path = entry.getValue();
            if (node.isLeaf()) {
                codes.put(node.value, path);
            } else {
                if (node.right != null) {
                    stack.push(new AbstractMap.SimpleEntry<>(node.right, path + '1'));
                }
                if (node.left != null) {
                    stack.push(new AbstractMap.SimpleEntry<>(node.left, path + '0'));
                }
            }
        }
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree(BitInputStream in) {
        root = deserialize(in);

        codes = new HashMap<>();
        Deque<AbstractMap.SimpleEntry<Node, String>> stack = new ArrayDeque<>();
        stack.push(new AbstractMap.SimpleEntry<>(root, ""));
        while (!stack.isEmpty()) {
            AbstractMap.SimpleEntry<Node, String> entry = stack.pop();
            Node node = entry.getKey();
            String path = entry.getValue();
            if (node.isLeaf()) {
                codes.put(node.value, path);
            } else {
                if (node.right != null) {
                    stack.push(new AbstractMap.SimpleEntry<>(node.right, path + '1'));
                }
                if (node.left != null) {
                    stack.push(new AbstractMap.SimpleEntry<>(node.left, path + '0'));
                }
            }
        }
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        serializeHelper(root, out);
    }

    private void serializeHelper(Node n, BitOutputStream out) {
        if (n.isLeaf()) {
            out.writeBit(0);
            out.writeBits(n.value, 9);
        } else {
            out.writeBit(1);
            serializeHelper(n.left, out);
            serializeHelper(n.right, out);
        }
    }

    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        int b;
        while ((b = in.readBits(8)) != -1) {
            String code = codes.get((short) b);
            for (char c : code.toCharArray()) {
                out.writeBit(c - '0');
            }
        }
        String eofCode = codes.get(EOF);
        for (char c : eofCode.toCharArray()) {
            out.writeBit(c - '0');
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) {
        Node cursor = root;
        int bit;
        while ((bit = in.readBit()) != -1) {
            cursor = (bit == 0 ? cursor.left : cursor.right);
            if (cursor.isLeaf()) {
                if (cursor.value == EOF) {
                    break;
                }
                out.writeBits(cursor.value, 8);
                cursor = root;
            }
        }
    }

    private Node deserialize(BitInputStream in) {
        int flag = in.readBit();
        if (flag < 0) {
            throw new IllegalArgumentException("Unexpected EOF in tree data");
        }
        if (flag == 0) {
            int v = in.readBits(9);
            if (v < 0) {
                throw new IllegalArgumentException("Unexpected EOF reading leaf");
            }
            return new Node((short) v, 0);
        } else {
            Node left = deserialize(in);
            Node right = deserialize(in);
            return new Node(left, right);
        }
    }
}