package coderead.maven.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@SpringBootApplication
@Controller
public class MavenJobApplication {
    @Autowired
    ClassIndexManager manager;

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext run = SpringApplication.run(MavenJobApplication.class, args);
    }

    @RequestMapping("/info")
    @ResponseBody
    public String getTaskInfo(){
        return manager.getTaskInfo();
    }

}
