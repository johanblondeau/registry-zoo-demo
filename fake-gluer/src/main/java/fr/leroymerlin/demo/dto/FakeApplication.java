package fr.leroymerlin.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor @NoArgsConstructor
public class FakeApplication {

    private String name;
    private String url;
    private int priority;

}
