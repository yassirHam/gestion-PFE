package entities;


import jakarta.persistence.*;

@Entity
@Table(name = "salle")
public class Salle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_salle;

    private String num_salle;
    private String block;
    private String status;

    public Salle() {}

	public Salle(Long id_salle, String num_salle, String block, String status) {
		super();
		this.id_salle = id_salle;
		this.num_salle = num_salle;
		this.block = block;
		this.status = status;
	}

	public Long getId_salle() {
		return id_salle;
	}

	public void setId_salle(Long id_salle) {
		this.id_salle = id_salle;
	}

	public String getNum_salle() {
		return num_salle;
	}

	public void setNum_salle(String num_salle) {
		this.num_salle = num_salle;
	}

	public String getBlock() {
		return block;
	}

	public void setBlock(String block) {
		this.block = block;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Salle [block=" + block + ", id_salle=" + id_salle + ", num_salle=" + num_salle + ", status=" + status
				+ "]";
	}
   
}