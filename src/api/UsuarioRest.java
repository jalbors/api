package api;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import domain.Authorization;
import domain.Constants;
import domain.Usuario;
import domain.UsuarioLogin;
import utils.HibernateProxyTypeAdapter;
import utils.HibernateUtil;

@Path("/usuarios")
public class UsuarioRest {

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response loginUsuario(String json) throws Exception {

		try {
			// empieza la transaccion
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			Session sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// recivo el usuario
			UsuarioLogin userLogin = gson.fromJson(json, UsuarioLogin.class);

			// le encripto las pass para que sea igual que en la BD
			String pass_encrip = encryptarSHA256(userLogin.getPassword()).trim();

			// creo una consulta pasandole el email y la pass encriptada
			Query<UsuarioLogin> consultaUserLogin = sesion.createQuery(
					"select new domain.UsuarioLogin(u.email, u.password) FROM Usuario as u WHERE u.email = :email AND u.password = :password",
					domain.UsuarioLogin.class);

			consultaUserLogin.setParameter("email", userLogin.getEmail());
			consultaUserLogin.setParameter("password", pass_encrip);

			// me devuelve el usuario si todo ha ido bien
			userLogin = (UsuarioLogin) consultaUserLogin.getSingleResult();

			// genera el token y setea la pass en blanco
			String tokenADevolver = generateToken(userLogin.getIdUser());
			userLogin.setToken(tokenADevolver);
			userLogin.setPassword("");

			// devuelvo el usuario
			String jsonADevolver = gson.toJson(userLogin);

			sesion.getTransaction().commit();
			sesion.close();
			return Response.status(200).entity(jsonADevolver).build();

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return Response.status(401).build();
		} catch (NoResultException e) {
			e.printStackTrace();
			// codigo de 300 para pruebas mias
			return Response.status(300).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(500).build();
		}
	}

	@GET
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsuarios(@PathParam("id") String id) {
		Session sesion = null;

		try {
			// creo una consulta que me devuelva los datos del ID elegido
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();
			Query<Usuario> consultaUsuarioActual = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.idUser = :idUser AND u.removeDate is NULL",
					domain.Usuario.class);
			consultaUsuarioActual.setParameter("idUser", Integer.parseInt(id));
			Usuario usuario = consultaUsuarioActual.getSingleResult();

			// muestro que haya devuelto lo correcto
			System.out.println(usuario.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// le envio el usuario
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuario);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}

	@GET
	@Path("/administradores")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdmins() {
		Session sesion = null;
		try {
			// creo y empiezo la transac
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// creo la consulta
			Query<Usuario> consultaUsers = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL AND u.rol = :rol",
					domain.Usuario.class);
			consultaUsers.setParameter("rol", "ADMIN");

			// hago una lista de todos los usuarios devueltos
			List<Usuario> usuariosTotales = consultaUsers.setMaxResults(999999999).getResultList();

			System.out.println(usuariosTotales.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// devuelvo la lista de usuarios totales
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuariosTotales);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}

	@GET
	@Path("/usuariosNormales")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersNormales() {
		Session sesion = null;
		try {
			// creo y empiezo la transac
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// creo la consulta
			Query<Usuario> consultaUsers = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL AND u.rol = :rol",
					domain.Usuario.class);
			consultaUsers.setParameter("rol", "USER");

			// hago una lista de todos los usuarios devueltos
			List<Usuario> usuariosTotales = consultaUsers.setMaxResults(999999999).getResultList();

			System.out.println(usuariosTotales.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// devuelvo la lista de usuarios totales
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuariosTotales);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}
	
	@GET
	@Path("/ordenarPorDinero")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCositas() {
		Session sesion = null;
		try {
			// creo y empiezo la transac
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// creo la consulta
			Query<Usuario> consultaUsers = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL order by u.money desc",
					domain.Usuario.class);

			// hago una lista de todos los usuarios devueltos
			List<Usuario> usuariosTotales = consultaUsers.setMaxResults(999999999).getResultList();

			System.out.println(usuariosTotales.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// devuelvo la lista de usuarios totales
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuariosTotales);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}
	
	@GET
	@Path("/ordenarPorFecha")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCositasVarias() {
		Session sesion = null;
		try {
			// creo y empiezo la transac
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// creo la consulta
			Query<Usuario> consultaUsers = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL order by u.registerDate asc",
					domain.Usuario.class);

			// hago una lista de todos los usuarios devueltos
			List<Usuario> usuariosTotales = consultaUsers.setMaxResults(999999999).getResultList();

			System.out.println(usuariosTotales.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// devuelvo la lista de usuarios totales
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuariosTotales);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}
	
	@GET
	@Path("/ordenarPorNombre")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrdenadosPorNombre() {
		Session sesion = null;
		try {
			// creo y empiezo la transac
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// creo la consulta
			Query<Usuario> consultaUsers = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL order by u.name asc",
					domain.Usuario.class);

			// hago una lista de todos los usuarios devueltos
			List<Usuario> usuariosTotales = consultaUsers.setMaxResults(999999999).getResultList();

			System.out.println(usuariosTotales.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// devuelvo la lista de usuarios totales
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuariosTotales);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsuariosTotales() {
		Session sesion = null;
		try {
			// creo y empiezo la transac
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// creo la consulta
			Query<Usuario> consultaUsers = sesion.createQuery(
					"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL",
					domain.Usuario.class);

			// hago una lista de todos los usuarios devueltos
			List<Usuario> usuariosTotales = consultaUsers.setMaxResults(999999999).getResultList();

			System.out.println(usuariosTotales.toString());
			sesion.getTransaction().commit();
			sesion.close();

			// devuelvo la lista de usuarios totales
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			String jsonADevolver = gson.toJson(usuariosTotales);
			return Response.status(200).entity(jsonADevolver).build();

		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return Response.status(400).build();
		} catch (NoResultException e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(404).build();
		} catch (Exception e) {
			sesion.close();
			e.printStackTrace();
			return Response.status(500).build();
		}

	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postCliente(String json, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {

		// si tiene el token continuara
		if (Authorization.isAuthorized(token)) {
			Session sesion = null;

			try {
				// creo la transac
				Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();

				// recibo el usuario, le creo una fecha para la BD,
				// le encripto la pass y le seteo un rol por defecto
				Usuario c1 = gson.fromJson(json, Usuario.class);
				c1.setRegisterDate(new Date());
				String pass = encryptarSHA256(c1.getPassword());
				c1.setPassword(pass);
				c1.setRol("USER");

				sesion.save(c1);
				sesion.getTransaction().commit();
				sesion.close();

				// le devuelvo el json con el usuario ya creado
				json = gson.toJson(c1);
				return Response.status(201).entity(json).build();

			} catch (JsonSyntaxException e) {
				sesion.close();
				e.printStackTrace();
				return Response.status(400).build();
			} catch (Exception e) {
				sesion.close();
				System.out.println(e.getMessage());
				return Response.status(500).build();
			}
		} else {
			return Response.status(401).build();
		}

	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response actualizarUsuario(@PathParam("id") String id, String json) {

		Session sesion = null;
		try {
			// creo la transa
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			// usuario que le envia el usuario
			Usuario u1 = gson.fromJson(json, Usuario.class);

			Query<Usuario> consulta = sesion.createQuery(
					"SELECT new domain.Usuario (u.name, u.surname, u.email, u.password, u.money) FROM Usuario as u WHERE u.idUser = :idUser",
					Usuario.class);
			consulta.setParameter("idUser", Integer.parseInt(id));

			// usuario ya creado en la BD
			Usuario usuario_comp = consulta.getSingleResult();

			// la pass del user la encripto porque en la BD esta encriptada
			String passUsuarioEnviada = encryptarSHA256(u1.getPassword());

			if (!passUsuarioEnviada.equalsIgnoreCase(usuario_comp.getPassword())
					&& !u1.getName().equalsIgnoreCase(usuario_comp.getName())
					&& !u1.getSurname().equalsIgnoreCase(usuario_comp.getSurname())
					&& !u1.getEmail().equalsIgnoreCase(usuario_comp.getEmail())
					&& u1.getMoney() != usuario_comp.getMoney()) {
				int numFilasAc = sesion.createQuery(
						"UPDATE domain.Usuario AS u SET u.password = :password, u.name = :name, u.surname = :surname, u.email = :email, u.money = :money WHERE u.idUser = :idUser")
						.setParameter("password", passUsuarioEnviada).setParameter("name", u1.getName())
						.setParameter("surname", u1.getSurname()).setParameter("email", u1.getEmail())
						.setParameter("money", u1.getMoney()).setParameter("idUser", Integer.parseInt(id))
						.executeUpdate();

				sesion.getTransaction().commit();
				sesion.close();

				String json_dev = gson.toJson(u1);
				System.out.println(numFilasAc);
				return Response.status(201).entity(json_dev).build();
			}

		} catch (JsonSyntaxException e) {

			sesion.close();
			System.out.println(e.getMessage());

			return Response.status(400).build();

		} catch (NoResultException e) {

			sesion.close();
			System.out.println(e.getMessage());
			return Response.status(404).build();

		} catch (HibernateException e) {

			sesion.close();
			System.out.println(e.getMessage());
			return Response.status(500).build();

		} catch (Exception e) {

			sesion.close();
			System.out.println(e.getMessage());
			return Response.status(500).build();
		}

		return Response.status(500).build();
	}

	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response eliminarFactura(@PathParam("id") String id, @HeaderParam(HttpHeaders.AUTHORIZATION) String token) {

		Session sesion = null;
		// pido el token
		if (Authorization.isAuthorized(token)) {
			try {
				// empieza la transa
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();

				// updateo el usuario para eliminarlo creadnole una fecha -> removeDate
				int numFilasActualizadas = sesion
						.createQuery(
								"UPDATE Usuario AS user SET user.removeDate = :removeDate WHERE user.idUser = :idUser")
						.setParameter("removeDate", new Date()).setParameter("idUser", Integer.parseInt(id))
						.executeUpdate();
				System.out.println(numFilasActualizadas);

				// si lo updatea devuelvo un 200
				sesion.getTransaction().commit();
				sesion.close();
				return Response.status(200).build();

			} catch (NoSuchFieldError e) {
				sesion.close();
				System.out.println(e.getMessage());
				return Response.status(404).build();
			} catch (Exception e) {
				sesion.close();
				System.out.println(e.getMessage());
				return Response.status(500).build();
			}
		}
		return Response.status(500).build();

	}

	// metodo para generar el token segun el ID
	public String generateToken(String id) throws Exception {
		SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd");

		Date issuedDate = formate.parse(LocalDate.now().toString());
		Date expirationDate = formate.parse(LocalDate.now().plusWeeks(1).toString());

		Algorithm algorithm = Algorithm.HMAC256(Constants.TOKEN_KEY);

		String token = JWT.create().withSubject(id).withIssuedAt(issuedDate).withExpiresAt(expirationDate)
				.sign(algorithm);

		return token;
	}

	// metodo para encriptar una contraseña recivida
	public String encryptarSHA256(String mensaje) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
			byte dataBytes[] = mensaje.getBytes();
			md.update(dataBytes);
			byte resumen[] = md.digest();
			return new String(Base64.getEncoder().encodeToString(resumen));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
}
