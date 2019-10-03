package io.quarkus.it.mongodb.panache.book;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@Path("/books/entity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookEntityResource {
    private static final Logger LOGGER = Logger.getLogger(BookEntityResource.class);

    @PostConstruct
    void init() {
        String databaseName = BookEntity.mongoDatabase().getName();
        String collectionName = BookEntity.mongoCollection().getNamespace().getCollectionName();
        LOGGER.infov("Using BookEntity[database={0}, collection={1}]", databaseName, collectionName);
    }

    @GET
    public List<BookEntity> getBooks(@QueryParam("sort") String sort) {
        if (sort != null) {
            return BookEntity.listAll(Sort.ascending(sort));
        }
        return BookEntity.listAll();
    }

    @POST
    public Response addBook(BookEntity book) {
        book.persist();
        String id = book.id.toString();
        return Response.created(URI.create("/books/entity/" + id)).build();
    }

    @PUT
    public Response updateBook(BookEntity book) {
        book.update();
        return Response.accepted().build();
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Response upsertBook(BookEntity book) {
        book.persistOrUpdate();
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    public void deleteBook(@PathParam("id") String id) {
        BookEntity theBook = BookEntity.findById(new ObjectId(id));
        theBook.delete();
    }

    @GET
    @Path("/{id}")
    public BookEntity getBook(@PathParam("id") String id) {
        return BookEntity.findById(new ObjectId(id));
    }

    @GET
    @Path("/search/{author}")
    public List<BookEntity> getBooksByAuthor(@PathParam("author") String author) {
        return BookEntity.list("author", author);
    }

    @GET
    @Path("/search")
    public BookEntity search(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return BookEntity.find("{'author': ?1,'bookTitle': ?2}", author, title).firstResult();
        }

        return BookEntity
                .find("{'creationDate': {$gte: ?1}, 'creationDate': {$lte: ?2}}", LocalDate.parse(dateFrom),
                        LocalDate.parse(dateTo))
                .firstResult();
    }

    @GET
    @Path("/search2")
    public BookEntity search2(@QueryParam("author") String author, @QueryParam("title") String title,
            @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo) {
        if (author != null) {
            return BookEntity.find("{'author': :author,'bookTitle': :title}",
                    Parameters.with("author", author).and("title", title)).firstResult();
        }

        return BookEntity.find("{'creationDate': {$gte: :dateFrom}, 'creationDate': {$lte: :dateTo}}",
                Parameters.with("dateFrom", LocalDate.parse(dateFrom)).and("dateTo", LocalDate.parse(dateTo))).firstResult();
    }

}