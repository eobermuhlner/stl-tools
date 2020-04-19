package ch.obermuhlner.stl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StlToolsApplication {
  private static final String NUMBER = "[-+]?(\\d*[.])?\\d+([eE][-+]?\\d+)?";
  private static final Pattern PATTERN_FACET_NORMAL = Pattern.compile("facet\\s+normal\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")");
  private static final Pattern PATTERN_VERTEX = Pattern.compile("vertex\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")");
  private static final Pattern PATTERN_ENDLOOP = Pattern.compile("endloop");

  public static void main(String[] args) {
    if (args.length == 0) {
      printHelp();
      return;
    }

    switch (args[0]) {
      case "binary":
        switch (args.length) {
          case 2 :
            convertToBinary(args[1]);
            return;
          case 3 :
            convertToBinary(args[1], args[2]);
            return;
        }
    }

    printHelp();
  }

  private static void printHelp() {
    System.out.println(""
        + "USAGE stltools binary <ascii-input-stl-file> [<binary-output-stl-file>]");
  }

  private static void convertToBinary(String inputFileName) {
    if (inputFileName.endsWith(".stl")) {
      String outputFileName = inputFileName.substring(0, inputFileName.length() - ".stl".length()) + "-binary.stl";
      convertToBinary(inputFileName, outputFileName);
    } else {
      System.err.println("Wrong input file format: " + inputFileName);
    }
  }

  private static void convertToBinary(String inputFileName, String outputFileName) {
    int triangleCount = 0;
    try (BufferedReader in = new BufferedReader(new FileReader(inputFileName))) {
      String line = in.readLine();
      while (line != null) {
        Matcher matcherFacetNormal = PATTERN_FACET_NORMAL.matcher(line);
        if (matcherFacetNormal.find()) {
          triangleCount++;
        }
        line = in.readLine();
      }
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }

    try (BufferedReader in = new BufferedReader(new FileReader(inputFileName))) {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFileName)))) {
        for (int i = 0; i < 80; i++) {
          out.writeByte(0);
        }

        writeInt32(out, triangleCount);

        String line = in.readLine();
        while (line != null) {
          Matcher matcherFacetNormal = PATTERN_FACET_NORMAL.matcher(line);
          if (matcherFacetNormal.find()) {
            String value1 = matcherFacetNormal.group(1);
            String value2 = matcherFacetNormal.group(4);
            String value3 = matcherFacetNormal.group(7);

            writeFloat32(out, Float.parseFloat(value1));
            writeFloat32(out, Float.parseFloat(value2));
            writeFloat32(out, Float.parseFloat(value3));
          } else {
            Matcher matcherVertex = PATTERN_VERTEX.matcher(line);
            if (matcherVertex.find()) {
              String value1 = matcherVertex.group(1);
              String value2 = matcherVertex.group(4);
              String value3 = matcherVertex.group(7);

              writeFloat32(out, Float.parseFloat(value1));
              writeFloat32(out, Float.parseFloat(value2));
              writeFloat32(out, Float.parseFloat(value3));
            } else {
              Matcher matcherEndLoop = PATTERN_ENDLOOP.matcher(line);
              if (matcherEndLoop.find()) {
                writeInt16(out, 0);
              }
            }
          }

          line = in.readLine();
        }
      }
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeInt16(DataOutputStream out, int value) throws IOException {
    out.writeByte(value & 0xFF);
    out.writeByte((value >> 8) & 0xFF);
  }

  private static void writeInt32(DataOutputStream out, int value) throws IOException {
    out.writeByte(value & 0xFF);
    out.writeByte((value >> 8) & 0xFF);
    out.writeByte((value >> 16) & 0xFF);
    out.writeByte((value >> 24) & 0xFF);
  }

  private static void writeFloat32(DataOutputStream out, float value) throws IOException {
    int intBits = Float.floatToRawIntBits(value);
    writeInt32(out, intBits);
  }
}
