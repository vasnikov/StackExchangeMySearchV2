package ru.rit.Controllers;

import com.google.code.stackexchange.schema.Question;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.*;
import ru.rit.QuestionsViaAPI;
import ru.rit.QuestionsViaJson;


@Controller
public class MyController {
    @GetMapping({"","search"})
    public String index(Model model, @ModelAttribute(value="tags") String tags,@ModelAttribute(value="mode") String mode,@ModelAttribute(value="pagenumber") String pageNumberStr,@ModelAttribute(value="pagesize") String pageSizeStr) throws IOException {

        if (tags.equals("")) { // если параметры поиска не заведены, то выводить нечего
            model.addAttribute("errors","Теги не указаны!!!");
            return "index";
        }

        String errors = null;

        // теги для отбора
        List<String> tagList= Arrays.asList(tags.split(";"));

        if (mode.equals("API")) {
            StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory.newInstance();

            Paging paging;
            try {
                paging = new Paging(Integer.parseInt(pageNumberStr), Integer.parseInt(pageSizeStr));
            } catch (Exception e) {
                paging = new Paging();
            }
            //Paging paging = new Paging(1, 2);

            //вопросы и ответы с телом
            String filter = "withbody";

            //получаем список вопросов с авторами и ответами
            QuestionsViaAPI questionsViaAPI = new QuestionsViaAPI(tagList, paging, filter);
            List<Question> questions = questionsViaAPI.getQuestions();
            errors = questionsViaAPI.getErrorStr();
            model.addAttribute("questions",questions);
        } else{
            QuestionsViaJson conn = new QuestionsViaJson(pageNumberStr, pageSizeStr);
            List<ru.rit.Generated.Question> questions = conn.fillQuestions(tags);
            errors = conn.getErrorStr();
            model.addAttribute("questions",questions);
        }

        model.addAttribute("tags",tags);
        model.addAttribute("pagenumber",pageNumberStr);
        model.addAttribute("pagesize",pageSizeStr);
        model.addAttribute("errors",errors);
        model.addAttribute("mode",mode);

        return "index";
    }
    @GetMapping({"parse"})
    public String parse(Model model, @ModelAttribute(value="tags") String tags, @ModelAttribute(value="pagenumber") String pageNumberStr, @ModelAttribute(value="pagesize") String pageSizeStr) throws IOException {
//        String myURL ="http://api.stackexchange.com/2.2/questions/7399154?order=desc&sort=activity&site=stackoverflow&filter=withbody";
//        StringBuilder sb = new StringBuilder();
//        URLConnection urlConn = null;
//        InputStreamReader in = null;
//        try {
//            URL url = new URL(myURL);
//            urlConn = url.openConnection();
//            if (urlConn != null)
//                urlConn.setReadTimeout(60 * 1000);
//            if (urlConn != null && urlConn.getInputStream() != null) {
//                in = new InputStreamReader(urlConn.getInputStream(),
//                        Charset.defaultCharset());
//                BufferedReader bufferedReader = new BufferedReader(in);
//                if (bufferedReader != null) {
//                    int cp;
//                    while ((cp = bufferedReader.read()) != -1) {
//                        sb.append((char) cp);
//                    }
//                    bufferedReader.close();
//                }
//            }
//            in.close();
//        } catch (Exception e) {
//            throw new RuntimeException("Exception while calling URL:"+ myURL, e);
//        }

        URL url = new URL("http://api.stackexchange.com/2.2/questions/7399154?order=desc&sort=activity&site=stackoverflow&filter=withbody");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-length", "0");
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Content-type", "Application/JSON");
        conn.connect();
        int rcode = conn.getResponseCode();
        String rmess = conn.getResponseMessage();

        BufferedReader in = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
        String inputLine;
        String inputJson="";
        while ((inputLine = in.readLine()) != null)
            inputJson = inputJson.concat(inputLine);
        in.close();

        return "index";
    }

}

