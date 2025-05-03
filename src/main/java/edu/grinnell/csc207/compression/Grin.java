package edu.grinnell.csc207.compression;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to outfile.
     * @param infile the file to decode
     * @param outfile the file to output to
     */
    public static void decode(String infile, String outfile) {
        try {
            BitInputStream in = new BitInputStream(infile);
            BitOutputStream out = new BitOutputStream(outfile);

            int magic = in.readBits(32);
            if (magic != 0x736) {
                throw new IllegalArgumentException("Not a .grin file (bad magic number)");
            }

            HuffmanTree tree = new HuffmanTree(in);
            tree.decode(in, out);

            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a frequency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) {
        Map<Short, Integer> freqs = new HashMap<>();
        try {
            BitInputStream in = new BitInputStream(file);
            int b;
            while ((b = in.readBits(8)) != -1) {
                short s = (short) b;
                freqs.put(s, freqs.getOrDefault(s, 0) + 1);
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return freqs;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile  the file to encode
     * @param outfile the file to write the output to
     */
    public static void encode(String infile, String outfile) {
        Map<Short, Integer> freqs = createFrequencyMap(infile);
        try {
            BitInputStream in = new BitInputStream(infile);
            BitOutputStream out = new BitOutputStream(outfile);

            out.writeBits(0x736, 32);

            HuffmanTree tree = new HuffmanTree(freqs);
            tree.serialize(out);
            tree.encode(in, out);

            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) {
        if (args.length != 3 || !(args[0].equals("encode") || args[0].equals("decode"))) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }

        if (args[0].equals("encode")) {
            encode(args[1], args[2]);
        } else {
            decode(args[1], args[2]);
        }
    }
}
