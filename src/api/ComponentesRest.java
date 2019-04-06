package api;

import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.hibernate.query.Query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import domain.Componentes;
import domain.Usuario;
import utils.HibernateProxyTypeAdapter;
import utils.HibernateUtil;

@Path("/componentes")
public class ComponentesRest {

	@GET
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getComponenteId(@PathParam("id") String id) {
		Session sesion = null;
		//if (Authorization.isAuthorized(token)) {

			try {
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();
				Query<Componentes> consultaUsuarioActual = sesion.createQuery(
						"select new domain.Componentes(u.idComponent,u.usuario, u.component, u.type, u.price, u.description, u.registerDate)FROM Usuario as u WHERE u.idComponent = :idComponent",
						domain.Componentes.class);
				consultaUsuarioActual.setParameter("idUser", Integer.parseInt(id));

				Componentes component = consultaUsuarioActual.getSingleResult();
				System.out.println(component.toString());
				sesion.getTransaction().commit();
				sesion.close();

				Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
				String jsonADevolver = gson.toJson(component);
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
	public Response getComponentes() {
			Session sesion = null;
			try {
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();
				Query<Componentes> consultaRobotsUser = sesion.createQuery(
						"select new domain.Componentes(u.idComponent,u.usuario, u.component, u.type, u.price, u.description, u.registerDate)FROM Usuario as u",
						domain.Componentes.class);

				List<Componentes> robotUsuario = consultaRobotsUser.setMaxResults(999999999).getResultList();

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
	public Response postComponente(String json) {
			Session sesion = null;
			try {
				Gson gson = new GsonBuilder().registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY).create();
				sesion = HibernateUtil.getSessionFactory().getCurrentSession();
				sesion.beginTransaction();

				Componentes c1 = gson.fromJson(json, Componentes.class);
				c1.setRegisterDate(new Date());
				//c1.setUsuario();
				//como setear el id del user al componente
				//c1.setUsuario(c1.getUsuario().getIdUser());

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
}
