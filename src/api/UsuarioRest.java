package api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
			Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
			Session sesion = HibernateUtil.getSessionFactory().getCurrentSession();
			sesion.beginTransaction();

			
			UsuarioLogin userLogin = gson.fromJson(json, UsuarioLogin.class);

			String pass_encrip = encryptarSHA256(userLogin.getPassword()).trim();
			
			Query<UsuarioLogin> consultaUserLogin = sesion.createQuery(
					"select new domain.UsuarioLogin(u.email, u.password) FROM Usuario as u WHERE u.email = :email AND u.password = :password",
					domain.UsuarioLogin.class);

			consultaUserLogin.setParameter("email", userLogin.getEmail());
			consultaUserLogin.setParameter("password", pass_encrip);

			
			userLogin = (UsuarioLogin) consultaUserLogin.getSingleResult();

			String tokenADevolver = generateToken(userLogin.getIdUser());
			userLogin.setToken(tokenADevolver);
			userLogin.setPassword("");
			
			String jsonADevolver = gson.toJson(userLogin);

			sesion.getTransaction().commit();
			sesion.close();

			return Response.status(200).entity(jsonADevolver).build();
			
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
			return Response.status(401).build();
		}catch(NoResultException e) {
			e.printStackTrace();
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
		//if (Authorization.isAuthorized(token)) {

			try {
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();
				Query<Usuario> consultaUsuarioActual = sesion.createQuery(
						"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.idUser = :idUser AND u.removeDate is NULL",
						domain.Usuario.class);
				consultaUsuarioActual.setParameter("idUser", Integer.parseInt(id));

				Usuario usuario = consultaUsuarioActual.getSingleResult();
				System.out.println(usuario.toString());
				sesion.getTransaction().commit();
				sesion.close();

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
		//}
		//return Response.status(500).build();
	}
	
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRobotsUsuario() {
			Session sesion = null;
			try {
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();
				Query<Usuario> consultaRobotsUser = sesion.createQuery(
						"select new domain.Usuario(u.idUser,u.name, u.surname, u.email, u.money, u.registerDate, u.rol)FROM Usuario as u WHERE u.removeDate is NULL",
						domain.Usuario.class);

				List<Usuario> robotUsuario = consultaRobotsUser.setMaxResults(999999999).getResultList();

				System.out.println(robotUsuario.toString());
				sesion.getTransaction().commit();
				sesion.close();

				Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
				String jsonADevolver = gson.toJson(robotUsuario);

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
	public Response postCliente(String json) {
			Session sesion = null;
			try {
				Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();

				Usuario c1 = gson.fromJson(json, Usuario.class);
				c1.setRegisterDate(new Date());
				String pass = encryptarSHA256(c1.getPassword());
				c1.setPassword(pass);
				c1.setRol("USER");

				sesion.save(c1);
				sesion.getTransaction().commit();
				sesion.close();

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
		
			

	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response actualizarUsuario(@PathParam("id") String id, String json) {

		Session sesion = null;
			try {

				Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();

				Usuario u1 = gson.fromJson(json, Usuario.class);

				Query<Usuario> consulta = sesion.createQuery(
						"SELECT new domain.Usuario (u.name, u.surname, u.email, u.password, u.money) FROM Usuario as u WHERE u.idUser = :idUser", Usuario.class);
				consulta.setParameter("idUser", Integer.parseInt(id));
				Usuario usuario_comp = consulta.getSingleResult();
				
				if (u1.getMoney() != usuario_comp.getMoney() || u1.getMoney() == usuario_comp.getMoney()) {

					int numFilasAc = sesion
							.createQuery(
									"UPDATE domain.Usuario AS u SET u.money = :money+u.money WHERE u.idUser = :idUser")
							.setParameter("money", u1.getMoney()).setParameter("idUser", Integer.parseInt(id))
							.executeUpdate();

					sesion.getTransaction().commit();
					sesion.close();

					String json_dev = gson.toJson(u1);

					return Response.status(201).entity(json_dev).build();

				}else if(u1.getPassword()!=usuario_comp.getPassword()) {
					int numFilasAc = sesion
							.createQuery(
									"UPDATE domain.Usuario AS u SET u.password = :password WHERE u.idUser = :idUser")
							.setParameter("password", u1.getPassword())
							.setParameter("idUser", Integer.parseInt(id))
							.executeUpdate();

					sesion.getTransaction().commit();
					sesion.close();

					String json_dev = gson.toJson(u1);

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
	public Response eliminarFactura(@PathParam("id") String id) {

		Session sesion = null;

			try {
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();

				sesion.beginTransaction();

				int numFilasActualizadas = sesion
						.createQuery(
								"UPDATE Usuario AS user SET user.removeDate = :removeDate WHERE user.idUser = :idUser")
						.setParameter("removeDate", new Date()).setParameter("idUser", Integer.parseInt(id))
						.executeUpdate();
				System.out.println(numFilasActualizadas);

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
	
	
	public String generateToken(String id) throws Exception {
		SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd");

		Date issuedDate = formate.parse(LocalDate.now().toString());
		Date expirationDate = formate.parse(LocalDate.now().plusWeeks(1).toString());

		Algorithm algorithm = Algorithm.HMAC256(Constants.TOKEN_KEY);

		String token = JWT.create().withSubject(id).withIssuedAt(issuedDate).withExpiresAt(expirationDate)
				.sign(algorithm);

		return token;
	}
	
	public String encryptarSHA256(String mensaje) throws NoSuchAlgorithmException {
		MessageDigest md;
			 md = MessageDigest.getInstance("SHA-256");

			 byte dataBytes[] = mensaje.getBytes();//TEXTO A BYTES
			 md.update(dataBytes) ;//SE INTRQDUCE TEXTO EN BYTES A RESUMIR
			 byte resumen[] = md.digest();//SE CALCULA EL RESUMEN

			 return new String(Base64.getEncoder().encodeToString(resumen));

	}
}
