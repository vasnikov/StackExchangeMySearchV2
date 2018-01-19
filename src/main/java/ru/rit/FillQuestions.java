package ru.rit;

import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.common.PagedList;
import com.google.code.stackexchange.schema.Answer;
import com.google.code.stackexchange.schema.Paging;
import com.google.code.stackexchange.schema.Question;
import com.google.code.stackexchange.schema.User;

import java.util.ArrayList;
import java.util.List;

public class FillQuestions {

    PagedList<Question> questions;
    String errorStr;

    public String getErrorStr() {
        return errorStr;
    }

    public PagedList<Question> getQuestions() {
        return questions;
    }

    /*
     * в конструкторе получаем все вопросы одним запросом
     * получаем всех авторов вопросов одним запросом
     * получаем всех ответы одним запросом
     * получаем всех авторов ответов одним запросом
     */
    public FillQuestions(List<String> tagList,Paging paging, String filter){
        StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory.newInstance();

        // запрашиваем список вопросов
        try{
            questions = queryFactory.newQuestionApiQuery()
                    .withPaging(paging)
                    .withFilter(filter)
//                .withTimePeriod(tp)
                    .withSort(Question.SortOrder.MOST_HOT)
                    .withTags(tagList)
                    .list();
        } catch (Exception e){
            errorStr.concat("\n\n").concat(e.getMessage());
        }
        //составляем список id авторов вопросов и список id принятых ответов
        List<Long> userIds = new ArrayList<>();
        List<Long> acceptedAnswerIds = new ArrayList<>();
        for (Question question: questions) {
            userIds.add(question.getOwner().getUserId());
            acceptedAnswerIds.add(question.getAcceptedAnswerId());
        }
        //получаем одним запросом всех авторов вопросов
        PagedList<User> users = null;
        try {
            users = queryFactory.newUserApiQuery().withUserIds(userIds).withFilter(filter).listUserByIds();
        } catch (Exception e){
            errorStr.concat("\n\n").concat(e.getMessage());
        }
        //вкладываем авторов в вопросы
        setUsersToQuestions(questions, users);

        //получаем одним запросом все ответы, если есть
        if (!acceptedAnswerIds.isEmpty()) {
            PagedList<Answer> acceptedAnswers = null;
            try {
                acceptedAnswers = queryFactory.newAnswerApiQuery().withAnswerIds(acceptedAnswerIds).listByIds();
            } catch (Exception e){
                errorStr.concat("\n\n").concat(e.getMessage());
            }
            //получаем одним запросом всех авторов всех принятых ответов
            List<Long> answerUserIds = new ArrayList<>();
            for (Answer answer : acceptedAnswers) {
                answerUserIds.add(answer.getOwner().getUserId());
            }
            PagedList<User> answerUsers = null;
            try{
                answerUsers = queryFactory.newUserApiQuery().withUserIds(answerUserIds).withFilter(filter).listUserByIds();
            } catch (Exception e){
                errorStr.concat("\n\n").concat(e.getMessage());
            }

            //вкладываем авторов в ответы
            setUsersToAnswers(acceptedAnswers, answerUsers);
            //вкладываем ответы в вопросы
            setAcceptedAnswerToQuestions(questions, acceptedAnswers);
        }
    }

    //вкладываем авторов в вопросы
    void setUsersToQuestions(PagedList<Question> entities, PagedList<User> users){
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
    void setUsersToAnswers(PagedList<Answer> answers, PagedList<User> users){
        for (Answer answer: answers) {
            for (User user: users) {
                if (answer.getOwner().getUserId()==user.getUserId()) {
                    answer.setOwner(user);
                    continue;
                }
            }
        }
    }

    //вкладываем ответы в вопросы
    void setAcceptedAnswerToQuestions(PagedList<Question> questions, PagedList<Answer> acceptedAnswers){
        for (Question question: questions) {
            for (Answer acceptedAnswer: acceptedAnswers) {
                if (question.getAcceptedAnswerId() > 0 && question.getAcceptedAnswerId()==acceptedAnswer.getAnswerId()) {
                    question.getAnswers().add(acceptedAnswer);
                    continue;
                }
            }
        }
    }
}

