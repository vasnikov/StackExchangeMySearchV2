package ru.rit;


import com.google.gson.*;
import org.jsoup.Jsoup;
import ru.rit.Generated.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/*
 *  Jsoap -> Gson -> Object
 */
public class QuestionsViaJson {
    String pageNumberStr;
    String pageSizeStr;
    String errorStr;

    public String getErrorStr() {
        return errorStr;
    }

    public QuestionsViaJson(String pageNumberStr, String pageSizeStr){
        this.pageNumberStr = pageNumberStr;
        this.pageSizeStr = pageSizeStr;
    }

    public List<Question> fillQuestions(String tags) throws IOException {
        // получаем все вопросы по запросу
        List<Question> questionList = getQuestions(tags);

        //составляем список id авторов вопросов и список id принятых ответов
        List<Integer> userIdList = new ArrayList<>();
        List<Integer> acceptedAnswerIdList = new ArrayList<>();
        for (Question question: questionList) {
            userIdList.add(question.getOwner().getUserId());
            if (question.getAcceptedAnswerId()>0)
                acceptedAnswerIdList.add(question.getAcceptedAnswerId());
        }
        //получаем одним запросом всех авторов вопросов
        List<User> userList = null;
        try {
            userList = getUsers(userIdList.toString().replace("[","").replace("]","").replace(",",";").replace(" ",""));
        } catch (Exception e){
            errorStr.concat("\n\n").concat(e.getMessage());
        }

        //вкладываем авторов в вопросы
        setUsersToQuestions(questionList, userList);

        //получаем одним запросом все ответы, если есть

        if (!acceptedAnswerIdList.isEmpty()) {
            List<Answer> acceptedAnswerList = null;
            try {
                acceptedAnswerList = getAnswers(acceptedAnswerIdList.toString().replace("[","").replace("]","").replace(",",";").replace(" ",""));
            } catch (Exception e){
                errorStr.concat("\n\n").concat(e.getMessage());
            }
            //получаем одним запросом всех авторов всех принятых ответов
            List<Integer> answerUserIdList = new ArrayList<>();
            for (Answer answer : acceptedAnswerList) {
                answerUserIdList.add(answer.getOwner().getUserId());
            }
            List<User> answerUserList = null;
            try{
                answerUserList = getUsers(answerUserIdList.toString().replace("[","").replace("]","").replace(",",";").replace(" ",""));
            } catch (Exception e){
                errorStr.concat("\n\n").concat(e.getMessage());
            }

            //вкладываем авторов в ответы
            setUsersToAnswers(acceptedAnswerList, answerUserList);
            //вкладываем ответы в вопросы
            setAcceptedAnswerToQuestions(questionList, acceptedAnswerList);
        }

        return questionList;
    }


    List<Question> getQuestions(String tags) throws IOException {
        String urlStr = "http://api.stackexchange.com/2.2/questions?page="+pageNumberStr+"&pagesize="+pageSizeStr+"&order=desc&sort=activity&tagged="+tags+"&site=stackoverflow&filter=withbody";
        System.out.println(urlStr);
        String json = Jsoup.connect(urlStr).ignoreContentType(true).execute().body();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, ser)
                .registerTypeAdapter(Date.class, deser).create();
        Questions questions = gson.fromJson(json, Questions.class);
        return questions.getQuestions();
    }

    List<Answer> getAnswers(String answersStr) throws IOException {
        String urlStr = "http://api.stackexchange.com/2.2/answers/"+answersStr+"?order=desc&sort=activity&site=stackoverflow&filter=withbody";
        System.out.println(urlStr);
        String json = Jsoup.connect(urlStr).ignoreContentType(true).execute().body();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, ser)
                .registerTypeAdapter(Date.class, deser).create();
        Answers answers = gson.fromJson(json, Answers.class);
        return answers.getAnswers();
    }

    List<User> getUsers(String usersStr) throws IOException {
        String urlStr = "https://api.stackexchange.com/2.2/users/"+usersStr+"?site=stackoverflow";
        System.out.println(urlStr);
        String json = Jsoup.connect(urlStr).ignoreContentType(true).execute().body();
        Gson gson = new Gson();
        Users users = gson.fromJson(json, Users.class);
        return users.getUsers();
    }

    //вкладываем авторов в вопросы
    void setUsersToQuestions(List<Question> entities, List<User> users){
        for (Question question: entities) {
            for (User user: users) {
                if (question.getOwner().getUserId()==user.getUserId()) {
                    question.setOwner(user);
                    continue;
                }
            }
        }
    }

    //вкладываем авторов в ответы
    void setUsersToAnswers(List<Answer> answerList, List<User> userList){
        for (Answer answer: answerList) {
            for (User user: userList) {
                if (answer.getOwner().getUserId()==user.getUserId()) {
                    answer.setOwner(user);
                    continue;
                }
            }
        }
    }

    //вкладываем ответы в вопросы
    void setAcceptedAnswerToQuestions(List<Question> questionList, List<Answer> acceptedAnswerList){
        for (Question question: questionList) {
            for (Answer acceptedAnswer: acceptedAnswerList) {
                if (question.getAcceptedAnswerId().equals(acceptedAnswer.getAnswerId())) {
                    question.setAnswers(Arrays.asList(acceptedAnswer));//question.getAnswers().add(acceptedAnswer);
                    continue;
                }
            }
        }
    }
    JsonSerializer<Date> ser = new JsonSerializer<Date>() {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                context) {
            return src == null ? null : new JsonPrimitive(src.getTime());
        }
    };

    JsonDeserializer<Date> deser = new JsonDeserializer<Date>() {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT,
                                JsonDeserializationContext context) throws JsonParseException {
            return json == null ? null : new Date(json.getAsLong());
        }
    };
}
