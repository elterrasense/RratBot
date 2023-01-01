package com.rrat.ogey.model;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public interface GenericMarkov extends Serializable {
    void update(List<String> token);
    Optional<List<String>> generate(RandomGenerator rng);
    Optional<List<String>> generate(String token, RandomGenerator rng);
}
