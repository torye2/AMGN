package amgn.amu.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "region")
public class Region {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private Long parentId;
	private Integer levelNo;
	private String path;
	public Integer getParentId() {
		// TODO Auto-generated method stub
		return null;
	}
	public Long getId() {
		// TODO Auto-generated method stub
		return null;
	}
	public Long getPath() {
		// TODO Auto-generated method stub
		return null;
	}
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	public String getLeveNo() {
		// TODO Auto-generated method stub
		return null;
	}
}
