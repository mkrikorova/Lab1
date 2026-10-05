package ru.itmo.spacemarine.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @NotNull
    @NotBlank(message = "Имя ордена не может быть пустым")
    @Column(name = "name", nullable = false)
    private String name;

    @Positive(message = "marinesCount должен быть больше 0")
    @Max(value = 1000, message = "marinesCount не может быть больше 1000")
    @Column(name = "marines_count", nullable = false)
    private long marinesCount;

    public Chapter() {
    }

    public Chapter(String name, long marinesCount) {
        this.name = name;
        this.marinesCount = marinesCount;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getMarinesCount() { return marinesCount; }
    public void setMarinesCount(long marinesCount) { this.marinesCount = marinesCount; }
}
