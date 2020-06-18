
 /**
  *
  * This class reads all the properties and writes the  setters and getters on the clipboard, 
  * The user has ctrl+v to paste the code.
  *
  *       @author: Shivam Gupta
  * 	  @date:   March 01, 2020
  *
  */

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.datatransfer.*;


public class SetterGetter{

 private static StringBuilder stringBuilder;
 private static RandomAccessFile randomAccessFile;
 private static File file;
 private static Clipboard clipboard;
 private static String[] identifiers;
 private static java.util.List<String> vars = new LinkedList<>();
 private static String className = "";
 private static java.util.List<String> dataTypes = new LinkedList<>();
 private static int choice = 0;
 
 public static boolean setFileName(String fileName) {
    try {
      file = new File(fileName);
      return file.exists();
    } catch( Exception exception ) {
        exception.printStackTrace();
        return false;
    }
 }

 public static void processFile() throws Exception {

    randomAccessFile=new RandomAccessFile(file, "rw");
    stringBuilder=new StringBuilder();
    long fileLength=randomAccessFile.length();
    String line;

    while(randomAccessFile.getFilePointer() < fileLength) {

      line=randomAccessFile.readLine().trim();   


      if(line.isEmpty()) continue;
      if(line.startsWith("/*") || line.startsWith("*") || line.startsWith("*/") || line.startsWith("//"))
        continue;
      if(line.startsWith("@")) continue;
      if(line.startsWith("package")) continue;
      if(line.startsWith("import")) continue;
      if(line.startsWith("public class")) {
        if(line.endsWith("{"))
          className = line.substring(line.indexOf("class") + 4, line.length() - 2).trim();
        else  
          className = line.substring(line.indexOf("class") + 4).trim();
        continue;
      }
      if(line.startsWith("protected class")) {
        if(line.endsWith("{"))
          className = line.substring(line.indexOf("class") + 5, line.length() - 2).trim();
        else  
          className = line.substring(line.indexOf("class") + 5).trim();
        continue;
      }
      if(line.startsWith("class")) {
        if(line.endsWith("{"))
          className = line.substring(line.indexOf("class") + 5, line.length() - 2).trim();
        else  
          className = line.substring(line.indexOf("class") + 5).trim();
        continue;
      }
      if(line.contains("(") || line.contains(")")) {
        while(randomAccessFile.getFilePointer() < fileLength && !line.endsWith("}")) {
            line = randomAccessFile.readLine().trim();
        }
        continue;
      }
      if(line.startsWith("{") || line.endsWith("{")) continue;
      if(line.endsWith("}")) continue;

      String[] lineIntoWords=line.split(" ");
      String word="";
      String accessSpecifier="";
      boolean dataTypeRead=false;
      boolean isStatic=false;
      boolean isFinal=false;
      String dataType="";

      for(int k = 0; k < lineIntoWords.length; ++k) {

          word=lineIntoWords[k];

          if(word.equals("static")) {
            isStatic=true;
            continue;
          }

          if(word.equals("final")) {
            isFinal=true;
            continue;
          }

          if(word.equals("private") || word.equals("public") || word.equals("protected"))
              continue;

          if(!dataTypeRead) {
              dataType=word;
              dataTypeRead=!dataTypeRead;
              continue;
          }

          if(word.equals("=")) {
            int m = k + 1;
            while(m < lineIntoWords.length && !(lineIntoWords[m].endsWith(",") || lineIntoWords[m].endsWith(";")))
              ++m;
            k = m;

          }

          else {
            identifiers=word.split(",");
            if(!isFinal) {
                writeSetterGetter(dataType, identifiers, isStatic);
            } else continue;   
          }
      }
    }

    if(choice == 1 || choice == 3) writeToString();
    if(choice == 2 || choice == 3) writeEqualsAndHashCode();

    copyToClipBoard();
    randomAccessFile.close();

 }

 public static void writeSetterGetter(String dataType, String[] identifiers, boolean isStatic) throws Exception {

    String t="";
    String k="";

    if(!isStatic) {

      for(String identifier : identifiers) {

          if( identifier.endsWith(";") ) identifier = identifier.substring(0,identifier.length()-1);
          
          t = identifier;
          vars.add(t);
          dataTypes.add(dataType);

          identifier = identifier.substring(0, 1).toUpperCase() + identifier.substring(1);

          stringBuilder.append( "\r\n\tpublic void set" + identifier + "(" + dataType + " " + t + ") {\r\n" );
          stringBuilder.append( "\t\tthis." + t + " = " + t + ";\r\n\t}\r\n" );
          stringBuilder.append( "\r\n\tpublic "+ dataType +" get" + identifier + "() {\r\n");
          stringBuilder.append( "\t\treturn this." + t + ";\r\n\t}\r\n" );
      }

    } 
    else {

      for(String identifier : identifiers) {
          if(identifier.endsWith(";"))
              identifier=identifier.substring(0,identifier.length()-1);

          t = "value";
          k = identifier;

          vars.add(k);
          dataTypes.add(dataType);

          identifier = identifier.substring(0, 1).toUpperCase() + identifier.substring(1);

          stringBuilder.append( "\r\n\tpublic static void set" + identifier + "(" + dataType + " " + t +") {\r\n");
          stringBuilder.append( "\t\t" + k + " = " + t + ";\r\n\t}\r\n");
          stringBuilder.append( "\r\n\tpublic static " + dataType + " get" + identifier + "() {\r\n");
          stringBuilder.append( "\t\treturn " + k + ";\r\n\t}\r\n");
      }
   }

 }

 public static void copyToClipBoard() throws Exception {

    clipboard=Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(new StringSelection(stringBuilder.toString()),null);

 }

 public static void writeToString() throws Exception {

    stringBuilder.append("\r\n\t@Override\r\n");
    stringBuilder.append("\tpublic String toString() {\r\n");
    stringBuilder.append("\t\tStringBuilder stringBuilder = new StringBuilder();\r\n");
    for(String v : vars) {
      String val = "\"" + v + ": \" + " + v + " + System.getProperty(\"line.separator\")";
      stringBuilder.append("\t\tstringBuilder.append("+val+");\r\n");
    }
    stringBuilder.append("\r\n\t\treturn stringBuilder.toString();\r\n\t}\r\n");

 }

 public static void writeEqualsAndHashCode() {

    stringBuilder.append("\r\n\t@Override\r\n");
    stringBuilder.append("\tpublic int hashCode() {\r\n");
    stringBuilder.append("\t\tint hash = 7;\r\n");
    int i = 0;
    for(String v : vars) {
        if(dataTypes.get(i).equals("int") || dataTypes.get(i).equals("Integer"))
            stringBuilder.append("\t\thash = 31 * hash + (int) " + v + ";\r\n");
        else if(dataTypes.get(i).equals("short") || dataTypes.get(i).equals("Short"))
            stringBuilder.append("\t\thash = 31 * hash + " + v + ";\r\n");
        else if(dataTypes.get(i).equals("byte") || dataTypes.get(i).equals("Byte"))
            stringBuilder.append("\t\thash = 31 * hash + " + v + ";\r\n");
        else if(dataTypes.get(i).equals("long") || dataTypes.get(i).equals("Long"))
            stringBuilder.append("\t\thash = 31 * hash + (int) " + v + ";\r\n");
        else if(dataTypes.get(i).equals("float") || dataTypes.get(i).equals("Float"))
            stringBuilder.append("\t\thash = 31 * hash + (int) " + v + ";\r\n");
        else if(dataTypes.get(i).equals("double") || dataTypes.get(i).equals("Double"))
            stringBuilder.append("\t\thash = 31 * hash + (int) " + v + ";\r\n");  
        else if(dataTypes.get(i).equals("char") || dataTypes.get(i).equals("Character"))
            stringBuilder.append("\t\thash = 31 * hash + (int) " + v + ";\r\n");
        else if(dataTypes.get(i).equals("boolean") || dataTypes.get(i).equals("Boolean"))
            stringBuilder.append("\t\thash = 31 * hash + (" + v + " ? 1 : 0);\r\n");
        else
            stringBuilder.append("\t\thash = 31 * hash + (" + v + " == null ? 0 : "+v+".hashCode());\r\n");

        ++i;                            

    }

    stringBuilder.append("\r\n\t\treturn hash;\n\r\t}\r\t");

    stringBuilder.append("\r\n\t@Override\r\n");
    stringBuilder.append("\tpublic boolean equals(Object object) {\r\n");
    stringBuilder.append("\t\tif( !(object instanceof "+className+") ) return false;\r\n");
    stringBuilder.append("\t\t"+className+" that = ("+className+") object;\r\n");
    stringBuilder.append("\t\treturn this.hashCode() == that.hashCode();\r\n\t}\r\n");

 }

 public static void main(String args[]) {

  if(args.length < 1) {
    System.out.println("[Usage: Path-to-file, <0, 1(toString), 2(equals, hash), 3(both)>]");
    return;
  }

  if(args.length >= 2)
    choice = Integer.parseInt(args[1]);    

  if( SetterGetter.setFileName(args[0]) ) {
   try {
       SetterGetter.processFile();
   } catch( Exception exception ) {
       exception.printStackTrace();
   }
  }
  else 
    System.err.println("Error!");
 }

}