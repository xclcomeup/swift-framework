package com.liepin.swift.framework.rpc.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.liepin.swift.core.util.AnnotationUtil;
import com.liepin.swift.framework.conf.SwiftConfig;
import com.liepin.swift.framework.rpc.config.InterfaceConfigWriter;

/**
 * 接口类重写编译器
 * 
 * @author yuanxl
 * 
 */
public class ClassRewriteCompiler {

    private static final String[] DEFAULT_EXCLUDES = new String[] {};
    // private static final String[] DEFAULT_INCLUDES = new String[] {
    // "**/service/*" };
    private String[] includes = new String[] { SwiftConfig.CLIENT_SERVICE };

    private String[] includeImpls = new String[] { SwiftConfig.CLIENT_SERVICE_IMPL };

    private File classesDir;

    private File tmpDir;

    private Map<File, File> moveMap = new HashMap<File, File>();

    private InterfaceConfigWriter writer;

    private FileClassLoader fileClassLoader;

    private String projectName = null;

    private Map<Class<?>, Map<Class<?>, File>> interfaceClasses = new HashMap<Class<?>, Map<Class<?>, File>>();

    public ClassRewriteCompiler(File directory) {
        this(null, directory);
    }

    public ClassRewriteCompiler(String projectName, File directory) {
        this.projectName = projectName;
        this.classesDir = directory;
        if (!classesDir.exists()) {
            throw new RuntimeException(directory + " not exists.");
        }
        if (!classesDir.isDirectory()) {
            throw new RuntimeException(directory + " not directory.");
        }
        this.tmpDir = new File(classesDir.getParentFile(), "tmp");
        tmpDir.mkdir();
        this.writer = new InterfaceConfigWriter(classesDir.getPath());
        this.fileClassLoader = new FileClassLoader(Thread.currentThread().getContextClassLoader(),
                classesDir.getPath());
    }

    /**
     * 编译
     * 
     * @throws IOException
     */
    public void compile() throws IOException {
        // 取接口文件
        List<File> interfaceFiles = directoryScan(includes);
        if (interfaceFiles.isEmpty()) {
            throw new RuntimeException(Arrays.toString(includes) + " package absent interface class");
        }

        // 取接口实现类文件
        List<File> interfaceImplFiles = directoryScan(includeImpls);
        if (interfaceImplFiles.isEmpty()) {
            throw new RuntimeException(Arrays.toString(includeImpls) + " package absent interface impl class");
        }

        // 过滤后需要打包的接口
        Set<File> pkgInterfaceFiles = new HashSet<File>(interfaceFiles.size());
        // 类加载
        for (File interfaceFile : interfaceFiles) {
            Class<?> interfaceClass = forClass(interfaceFile, null);
            // 过滤非打包接口类
            if (!AnnotationUtil.hasSwiftService(interfaceClass)) {
                continue;
            }
            interfaceClasses.put(interfaceClass, new HashMap<Class<?>, File>());
            pkgInterfaceFiles.add(interfaceFile);
        }
        for (File interfaceImplFile : interfaceImplFiles) {
            Class<?> interfaceImplClass = forClass(interfaceImplFile, getClassName(interfaceImplFile));
            for (Map.Entry<Class<?>, Map<Class<?>, File>> entry : interfaceClasses.entrySet()) {
                Class<?> interfaceClass = entry.getKey();
                Map<Class<?>, File> interfaceImplClasses = entry.getValue();
                if (interfaceClass.isAssignableFrom(interfaceImplClass)) {
                    interfaceImplClasses.put(interfaceImplClass, interfaceImplFile);
                }
            }
        }

        // 写config
        if (projectName != null) {
            writer.write(projectName);
        }
        for (Map.Entry<Class<?>, Map<Class<?>, File>> entry : interfaceClasses.entrySet()) {
            Class<?> interfaceClass = entry.getKey();
            Map<Class<?>, File> values = entry.getValue();
            if (values.isEmpty()) {
                throw new RuntimeException("Interface Class " + interfaceClass + " absent impl class");
            }
            // 过滤非打包接口类
            if (!AnnotationUtil.hasSwiftService(interfaceClass)) {
                continue;
            }
            writer.write(interfaceClass, values);
            System.out.println("Added {" + interfaceClass.getName() + "} to config\n");
        }
        writer.close();

        // 复写类
        for (File interfaceFile : interfaceFiles) {
            // move 到 临时目录
            // File targetFile = new File(new File(tmpDir,
            // interfaceFile.getParent()), interfaceFile.getName());
            File targetFile = new File(tmpDir, getRelativePath(interfaceFile));
            FileUtils.copyFile(interfaceFile, targetFile);
            moveMap.put(interfaceFile, targetFile);

            if (pkgInterfaceFiles.contains(interfaceFile)) {
                File parentDir = interfaceFile.getParentFile();
                // 生成新的class字节流
                byte[] newInterfaceClassbytes = rewriteInterfaceClass(interfaceFile);
                // 注意：必须另起class loader
                // BytesClassLoader bytesClassLoader = new
                // BytesClassLoader(Thread.currentThread().getContextClassLoader());
                // Class<?> interfaceClass =
                // bytesClassLoader.findClass(newInterfaceClassbytes);

                // 生成新的类
                File newInterfaceFile = new File(parentDir, interfaceFile.getName());
                FileOutputStream fout = new FileOutputStream(newInterfaceFile);
                fout.write(newInterfaceClassbytes);
                fout.close();
            } else {
                System.out.println("compile ignore file: " + interfaceFile);
                interfaceFile.delete();
            }
        }
    }

    /**
     * 复写类字节流
     * 
     * @param is
     * @return
     * @throws IOException
     */
    private byte[] rewriteInterfaceClass(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        final Set<Integer> methodNumbers = new LinkedHashSet<Integer>();
        ClassReader cr = new ClassReader(is);
        ClassAdapter classAdapter = new MethodCollectClassAdapter(methodNumbers,
                new ClassWriter(ClassWriter.COMPUTE_MAXS));
        cr.accept(classAdapter, ClassReader.SKIP_DEBUG);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassAdapter classAdapter1 = new MethodDeleteClassAdapter(methodNumbers, cw);
        cr.accept(classAdapter1, ClassReader.SKIP_DEBUG);
        is.close();
        return cw.toByteArray();
    }

    /**
     * 过滤扫描目录
     * 
     * @return
     * @throws IOException
     */
    private List<File> directoryScan(String[] includes) throws IOException {
        List<File> files = new ArrayList<File>();
        PlexusIoFileResourceCollection collection = new PlexusIoFileResourceCollection();
        collection.setIncludes(includes);
        collection.setExcludes(DEFAULT_EXCLUDES);
        collection.setBaseDir(this.classesDir);
        collection.setFileSelectors(null);
        collection.setIncludingEmptyDirectories(true);
        collection.setPrefix("");
        collection.setCaseSensitive(true);
        collection.setUsingDefaultExcludes(true);
        Iterator<?> resources = collection.getResources();
        while (resources.hasNext()) {
            PlexusIoFileResource resource = (PlexusIoFileResource) resources.next();
            if (resource.isDirectory()) {
                continue;
            }
            files.add(resource.getFile());
            // 控制台输出
            System.out.println("classes directory scan add: " + resource.getFile());
        }
        return files;
    }

    /**
     * 清理现场并删除临时目录
     * 
     * @throws IOException
     */
    public void clean() throws IOException {
        for (Map.Entry<File, File> entry : moveMap.entrySet()) {
            // move还原
            FileUtils.copyFile(entry.getValue(), entry.getKey());
        }
        // 清除临时目录
        FileUtils.deleteDirectory(tmpDir);
        // 删除临时打包的配置文件
        writer.getFile().delete();
    }

    /**
     * 从文件转换为类
     * 
     * @param file
     * @return
     * @throws IOException
     */
    private Class<?> forClass(File file, String className) throws IOException {
        byte[] newInterfaceClassbytes = FileUtils.readFileToByteArray(file);
        return fileClassLoader.findClass(newInterfaceClassbytes, className);
    }

    /**
     * 根据文件路径反解析文件类名
     * 
     * @param file
     * @return
     */
    private String getClassName(File file) {
        String temp = getRelativePath(file);
        temp = temp.replaceAll("[\\" + File.separator + "]", ".");
        temp = temp.substring(0, temp.length() - ".class".length());
        return temp;
    }

    /**
     * 相对路径
     * 
     * @param file
     * @return
     */
    private String getRelativePath(File file) {
        String classPath = classesDir.getPath();
        String filePath = file.getPath();
        return filePath.substring(classPath.length() + 1);
    }

}
