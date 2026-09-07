package ipiranga.fatec.qrvet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "clinics")
public class Clinic {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    protected Clinic() {}

    public Clinic(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
}
