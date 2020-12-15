package org.jeecg.common.util.shell;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ShellUtils
 * @Description: Shell 工具类
 * @Author: bruceouyang
 * @Date: 2020/5/7 下午5:04
 * @Version: V1
 **/
@Slf4j
public class ShellUtils {

    public static List<String> execCmdWithOutput(String cmd) {
        Process process;
        List<String> infoList = new ArrayList<>();
        try {
            process = Runtime.getRuntime().exec(cmd);
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = "";
            while((line = reader.readLine()) != null) {
                infoList.add(line);
            }
            reader.close();
            inputStreamReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        infoList.forEach(info -> log.info(info));
        return infoList;
    }

    /**
     * 执行 shell 命令
     * @param cmd
     */
    public static String execCmd(String cmd) {
        StringBuffer sb = new StringBuffer();
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            int exitValue = process.waitFor();
            if (0 != exitValue) {
                log.info("call shell `{}` failed. error code is {} ", cmd, exitValue);
//                return "-1";
            }
            InputStream in = process.getInputStream();
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(reader);
            String message;
            while((message = br.readLine()) != null) {
                sb.append(message);
            }
        } catch (IOException | InterruptedException e) {
            log.error("call shell `{}` failed, error msg is {}", cmd, e.getMessage());
        }
        return sb.toString();
    }
}
