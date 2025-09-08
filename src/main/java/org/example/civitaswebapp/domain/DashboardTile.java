package org.example.civitaswebapp.domain;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String type;

    private int positionX = 0;
    private int positionY = 0;
    private int width = 1;
    private int height = 1;

    @ManyToOne
    @JsonBackReference
    private MyUser user;
}
