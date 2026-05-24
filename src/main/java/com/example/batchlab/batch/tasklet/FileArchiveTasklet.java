package com.example.batchlab.batch.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FileArchiveTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(FileArchiveTasklet.class);

    @Value("${app.batch.input-dir}")
    private String inputDir;

    @Value("${app.batch.archive-dir}")
    private String archiveDir;

    /**
     * 是否保留源文件。
     * true  = 复制到归档目录，原始 input 文件保留（学习/反复演练用，默认 local）。
     * false = 移动到归档目录，删除原始文件（生产环境的"世代归档"真实语义）。
     */
    @Value("${app.batch.archive.keep-source:false}")
    private boolean keepSource;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("--- Starting File Archive Tasklet ---");

        File inputFolder = new File(inputDir);
        File[] files = inputFolder.listFiles((dir, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            log.info("No CSV files found in input directory to archive.");
            return RepeatStatus.FINISHED;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = dtf.format(LocalDateTime.now());

        // 确保归档目录存在（防止 NoSuchFileException）
        Path archivePath = Paths.get(archiveDir);
        if (!Files.exists(archivePath)) {
            Files.createDirectories(archivePath);
            log.info("Created archive directory: {}", archivePath);
        }

        for (File file : files) {
            String originalName = file.getName();
            String nameWithoutExt = originalName.substring(0, originalName.lastIndexOf("."));
            String extension = originalName.substring(originalName.lastIndexOf("."));
            
            // 构造新的归档文件名: sample_20260516142000.csv
            String newName = nameWithoutExt + "_" + timestamp + extension;
            Path sourcePath = file.toPath();
            Path targetPath = Paths.get(archiveDir, newName);

            if (keepSource) {
                // 复制：归档一份，原始文件保留（演练时可反复运行，不会"跑一次就没了"）
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Archived (copy, source kept): {} -> {}", originalName, targetPath.toString());
            } else {
                // 移动：归档后删除原始文件（生产环境的世代归档语义）
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Archived (move, source removed): {} -> {}", originalName, targetPath.toString());
            }
        }

        log.info("--- File Archive Tasklet Completed ---");
        return RepeatStatus.FINISHED;
    }
}
