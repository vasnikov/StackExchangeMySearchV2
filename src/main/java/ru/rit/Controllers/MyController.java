package ru.rit.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Arrays;
import java.util.List;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.common.PagedList;
import com.google.code.stackexchange.schema.*;
import ru.rit.FillQuestions;




@Controller
//@RequestMapping(value = "/rit")
public class MyController {
    @GetMapping({"","search"})
    public String index(Model model, @ModelAttribute(value="tags") String tags, @ModelAttribute(value="pagenumber") String pageNumberStr, @ModelAttribute(value="pagesize") String pageSizeStr) {

        if (tags.equals("")) // если параметры поиска не заведены, то выводить нечего
            return "index";

        StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory.newInstance();

        //
        Paging paging;
        try {
            paging = new Paging(Integer.parseInt(pageNumberStr), Integer.parseInt(pageSizeStr));
        } catch (Exception e) {
            paging = new Paging();
        }
        //Paging paging = new Paging(1, 2);

        //вопросы и ответы с телом
        String filter = "withbody";

        // теги для отбора
        List<String> tagList= Arrays.asList(tags.split(","));

        //получаем список вопросов с авторами и ответами
        FillQuestions fillQuestions = new FillQuestions(tagList,paging,filter);
        PagedList<Question> questions = fillQuestions.getQuestions();

        model.addAttribute("tags",tags);
        model.addAttribute("pagenumber",pageNumberStr);
        model.addAttribute("pagesize",pageSizeStr);
        model.addAttribute("errors",fillQuestions.getErrorStr());
        model.addAttribute("questions",questions);

        return "index";
    }
}

