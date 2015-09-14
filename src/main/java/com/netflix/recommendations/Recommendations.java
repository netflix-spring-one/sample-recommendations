package com.netflix.recommendations;

import java.util.Set;

import javax.inject.Inject;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@SpringBootApplication
@EnableCircuitBreaker
@EnableEurekaClient
@EnableFeignClients
public class Recommendations {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Recommendations.class).web(true).run(args);
    }
}

@FeignClient("membership")
interface MembershipRepository {
    @RequestMapping(method = RequestMethod.GET, value = "/api/member/{user}")
    Member findMember(@PathVariable("user") String user);
}

@RestController
@RequestMapping("/api/recommendations")
class RecommendationsController {
    @Inject
    MembershipRepository membershipRepository;

    Set<Movie> kidRecommendations = Sets.newHashSet(new Movie("lion king"), new Movie("frozen"));
    Set<Movie> adultRecommendations = Sets.newHashSet(new Movie("shawshank redemption"), new Movie("spring"));
    Set<Movie> familyRecommendations = Sets.newHashSet(new Movie("hook"), new Movie("the sandlot"));

    @RequestMapping("/{user}")
    @HystrixCommand(fallbackMethod = "recommendationFallback")
    public Set<Movie> findRecommendationsForUser(@PathVariable String user) {
        Member member = membershipRepository.findMember(user);
        if(member == null)
            return familyRecommendations;
        return member.age < 17 ? kidRecommendations : adultRecommendations;
    }

    /**
     * Should be safe for all audiences
     */
    Set<Movie> recommendationFallback(String user) {
        return familyRecommendations;
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