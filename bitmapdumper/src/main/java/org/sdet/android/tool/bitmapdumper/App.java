package org.sdet.android.tool.bitmapdumper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collections;

import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaField;
import com.sun.tools.hat.internal.model.JavaHeapObject;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;
import com.sun.tools.hat.internal.model.JavaValueArray;
import com.sun.tools.hat.internal.model.Snapshot;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class App {
    
    private static String BitmapClassName = "android.graphics.Bitmap";
    
    public static void main(String[] args) {

        // 解析命令行
        CommandLineParser parser = new PosixParser();
        Options options = new Options();  
        options.addOption("h", "help", false, "help");  
        options.addOption("s", "source", true, "source hprof file");
        options.addOption("c", "compare", false, "compare to this hprof file");
        options.addOption("o", "output", true, "output directory");
        
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);          
        } catch (ParseException e) {
//            HelpFormatter formatter = new HelpFormatter();
//            formatter.printHelp( "BitmapDumper", options);
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        
        if (commandLine.hasOption("h") || ! commandLine.hasOption("s") || ! commandLine.hasOption("o")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "BitmapDumper", options);
            System.exit(-1);
        }
        
        String targetPath = commandLine.getOptionValue("s");
        String comparePath = commandLine.hasOption("c")? commandLine.getOptionValue("c"): null;
        
        // 分析hprof文件
        Snapshot targetSnapshot = null;
        Snapshot compareSnaphost = null;
        ArrayList<String> excludes = new ArrayList<String>();
        
        try {
            targetSnapshot = com.sun.tools.hat.internal.parser.Reader.readFile(targetPath, true, 0);
            targetSnapshot.resolve(true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.err.println(String.format("error in reading hprof file [%s]", targetPath));
        }
        
        if (comparePath != null) {
            try {
                compareSnaphost = com.sun.tools.hat.internal.parser.Reader.readFile(targetPath, true, 0);
                compareSnaphost.resolve(true);
                JavaClass klass = compareSnaphost.findClass(BitmapClassName);
                excludes = Collections.list(klass.getInstances(false));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.err.println(String.format("error in reading hprof file [%s]", targetPath));
            }
        }
        
        JavaClass bitmapClass = targetSnapshot.findClass(BitmapClassName);        
        Enumeration iterator = bitmapClass.getInstances(false);
        while (iterator.hasMoreElements()) {            
            JavaObject object = (JavaObject) iterator.nextElement();          
            if (commandLine.hasOption("c") && excludes != null && excludes.contains(object.toString())) continue;
            
            byte[] bytes = {0x0};
            String height = "";
            String width = "";
            
            final JavaThing[] things = object.getFields();
            final JavaField[] fields = object.getClazz().getFieldsForInstance();
                       
            for (int i = 0; i < fields.length; i++) {               
                String fieldName = fields[i].getName();
                if (fieldName.equals("mBuffer")) {
                    if (things[i].getClass().getSimpleName().equals("JavaValueArray")) {
                        bytes = (byte[]) ((JavaValueArray) things[i]).getElements();
                    } else {
                        continue;
                    }                   
                } else if (fieldName.equals("mHeight")) {
                    height = things[i].toString();
                } else if (fieldName.equals("mWidth")) {
                    width = things[i].toString();
                }
            }
            
            int bits = bytes.length / Integer.parseInt(width) / Integer.parseInt(height) * 8;
            File outputFolder = new File(commandLine.getOptionValue("o"), "rgb");
            File rgbFile = new File(outputFolder, String.format("%s_%sx%s_%d.rgb", object.toString(), width, height, bits));
            try {
                FileOutputStream out = new FileOutputStream (rgbFile);
                out.write(bytes);
                out.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            
        }
        
        

        
    }
    
    
}
