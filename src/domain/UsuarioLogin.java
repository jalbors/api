package domain;

public class UsuarioLogin {

	private String email;
	private String password;
	private String token;
	private String idUser;

	public UsuarioLogin(String email, String password) {
		super();
		this.email = email;
		this.password = password;
	}

	public UsuarioLogin(String email, String password, String token) {
		super();
		this.email = email;
		this.password = password;
		this.token = token;
	}

	public String getIdUser() {
		return idUser;
	}

	public void setIdUser(String idUser) {
		this.idUser = idUser;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
