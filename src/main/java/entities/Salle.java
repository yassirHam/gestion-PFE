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

    /** Owning department (governance layer). null = global / shared. */
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "campus", length = 128)
    private String campus;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "equipment", length = 1024)
    private String equipment;

    /** Higher priority salles are picked first (for VIP defenses or limited rooms). */
    @Column(name = "priority")
    private int priority;

    /** When false, the salle is excluded from planning even though it exists. */
    @Column(name = "available")
    private boolean available = true;

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

	public Department getDepartment() { return department; }
	public void setDepartment(Department v) { this.department = v; }

	public String getCampus() { return campus; }
	public void setCampus(String campus) { this.campus = campus; }

	public Integer getCapacity() { return capacity; }
	public void setCapacity(Integer capacity) { this.capacity = capacity; }

	public String getEquipment() { return equipment; }
	public void setEquipment(String equipment) { this.equipment = equipment; }

	public int getPriority() { return priority; }
	public void setPriority(int priority) { this.priority = priority; }

	public boolean isAvailable() { return available; }
	public void setAvailable(boolean v) { this.available = v; }

	@Override
	public String toString() {
		return "Salle [block=" + block + ", id_salle=" + id_salle + ", num_salle=" + num_salle + ", status=" + status
				+ "]";
	}
   
}
