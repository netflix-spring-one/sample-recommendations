package com.netflix.recommendations;

import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Sets;

@SpringBootApplication
@EnableEurekaClient
public class Recommendations {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Recommendations.class).web(true).run(args);
    }
}

@RestController
@RequestMapping("/api/recommendations")
class RecommendationsController {
    @Autowired
    RestTemplate restTemplate;

    Set<Movie> kidRecommendations = Sets.newHashSet(new Movie("lion king"), new Movie("frozen"));
    Set<Movie> adultRecommendations = Sets.newHashSet(new Movie("shawshank redemption"), new Movie("spring"));

    @RequestMapping("/{user}")
    public Set<Movie> findRecommendationsForUser(@PathVariable String user) throws UserNotFoundException {
        Member member = restTemplate.getForObject("http://members/api/member/{user}", Member.class, user);
        if(member == null)
            throw new UserNotFoundException();
        return member.age < 17 ? kidRecommendations : adultRecommendations;
    }
}

@Data
class Movie {
    final String title;
}

@Data
@NoArgsConstructor // for jackson
class Member {
    String user;
    Integer age;
}

class UserNotFoundException extends Exception {}