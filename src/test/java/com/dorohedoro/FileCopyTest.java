package com.dorohedoro;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.stream.IntStream;

@Slf4j
public class FileCopyTest {

    private final int ROUNDS = 5;

    interface FileCopyRunner {

        void copy(File source, File target);
    }

    FileCopyRunner noBufferIOStreamCopy = (source, target) -> {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            int res;
            while ((res = in.read()) != -1) {
                out.write(res);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    };

    FileCopyRunner bufferIOStreamCopy = (source, target) -> {
        try (InputStream in = new BufferedInputStream(new FileInputStream(source));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[1024];
            int res;
            while ((res = in.read(buffer)) != -1) {
                out.write(buffer, 0, res);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    };

    FileCopyRunner nioBufferCopy = (source, target) -> {
        try (FileChannel in = new FileInputStream(source).getChannel();
             FileChannel out = new FileOutputStream(target).getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (in.read(buffer) != -1) {
                buffer.flip(); // 写模式转为读模式
                while (buffer.hasRemaining()) {
                    out.write(buffer);
                }
                buffer.clear(); // 读模式转为写模式
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    };

    FileCopyRunner nioTransferCopy = (source, target) -> {
        try (FileChannel in = new FileInputStream(source).getChannel();
             FileChannel out = new FileOutputStream(target).getChannel()) {
            long transferred = 0L;
            while (transferred != in.size()) {
                transferred += in.transferTo(0, in.size(), out);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    };
    
    @Test
    public void copy() {
        File small = new File("C:\\mybatis-plus\\small");
        File smallCopy = new File("C:\\mybatis-plus\\small_copy");
        log.info("拷贝1M大小的文件");
        benchmark(bufferIOStreamCopy, small, smallCopy);
        benchmark(nioBufferCopy, small, smallCopy);
        benchmark(nioTransferCopy, small, smallCopy);

        File big = new File("C:\\mybatis-plus\\big");
        File bigCopy = new File("C:\\mybatis-plus\\big_copy");
        log.info("拷贝100M大小的文件");
        benchmark(bufferIOStreamCopy, big, bigCopy);
        benchmark(nioBufferCopy, big, bigCopy);
        benchmark(nioTransferCopy, big, bigCopy);
    }

    public void benchmark(FileCopyRunner runner, File source, File target) {
        long start = System.currentTimeMillis();
        IntStream.range(0, ROUNDS).forEach(o -> {
            runner.copy(source, target);
            target.delete();
        });
        log.info(runner + ": " + (System.currentTimeMillis() - start) / ROUNDS + "ms");
    }
}
