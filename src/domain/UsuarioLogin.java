package domain;

//Usuario para el login
public class UsuarioLogin {

	private String email;
	private String password;
	private String token;
	private String idUser;
	private String rol;

	public UsuarioLogin() {
		super();
	}

	public UsuarioLogin(String email, String password) {
		super();
		this.email = email;
		this.password = password;
	}

	public UsuarioLogin(String email, String password, String rol) {
		super();
		this.email = email;
		this.password = password;
		this.rol = rol;
	}

	public UsuarioLogin(String email, String password, String token, String rol) {
		super();
		this.email = email;
		this.password = password;
		this.token = token;
		this.rol = rol;
	}

	public String getRol() {
		return rol;
	}

	public void setRol(String rol) {
		this.rol = rol;
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
