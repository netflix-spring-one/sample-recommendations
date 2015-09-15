package com.netflix.recommendations;

import java.util.Set;

import com.netflix.governator.annotations.binding.Primary;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaStatusChangedEvent;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Sets;
import com.netflix.appinfo.InstanceInfo;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableHystrix
public class Recommendations {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Recommendations.class).web(true).run(args);
    }

    @EventListener
    public void onEurekaStatusDown(EurekaStatusChangedEvent event) {
        if(event.getStatus() == InstanceInfo.InstanceStatus.DOWN || event.getStatus() == InstanceInfo.InstanceStatus.OUT_OF_SERVICE) {
            System.out.println("Stop listening to queues and such...");
        }
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
    @Autowired
    MembershipRepository membershipRepository;

    Set<Movie> kidRecommendations = Sets.newHashSet(new Movie("lion king"), new Movie("frozen"));
    Set<Movie> adultRecommendations = Sets.newHashSet(new Movie("shawshank redemption"), new Movie("spring"));
    Set<Movie> familyRecommendations = Sets.newHashSet(new Movie("hook"), new Movie("the sandlot"));

    @RequestMapping("/{user}")
    @HystrixCommand(fallbackMethod = "recommendationFallback", commandProperties={
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    })
    public Set<Movie> findRecommendationsForUser(@PathVariable String user) throws UserNotFoundException {
        Member member = membershipRepository.findMember(user);
        if(member == null)
            throw new UserNotFoundException();
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

class UserNotFoundException extends Exception {}