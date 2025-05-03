package edu.grinnell.csc207.compression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Tests {
  @TempDir
  Path temp;

  @Test
  void testCreateFrequencyMap() throws IOException {
    Path in = temp.resolve("freq.txt");
    Files.write(in, "aba ".getBytes());
    Map<Short,Integer> freq = Grin.createFrequencyMap(in.toString());
    assertEquals(2, freq.get((short)'a'));
    assertEquals(1, freq.get((short)'b'));
    assertEquals(1, freq.get((short)' '));
  }

  @Test
  void testEncodeDecodeRoundTrip() throws IOException {
    Path in = temp.resolve("input.txt");
    String text = "the quick brown fox jumps over the lazy dog";
    Files.write(in, text.getBytes());

    Path grin = temp.resolve("out.grin");
    Grin.encode(in.toString(), grin.toString());
    Path out = temp.resolve("output.txt");
    Grin.decode(grin.toString(), out.toString());
    byte[] orig = Files.readAllBytes(in);
    byte[] round = Files.readAllBytes(out);
    assertArrayEquals(orig, round);
  }

  @Test
  void testDecodeWithBadMagicThrows() throws IOException {
    Path fake = temp.resolve("notgrin.txt");
    Files.write(fake, "hello".getBytes());
    Path out = temp.resolve("shouldfail.txt");
    assertThrows(IllegalArgumentException.class, () ->
      Grin.decode(fake.toString(), out.toString())
    );
  }
}
