package ua.kiev.prog.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class DbManager {

    private static EntityManagerFactory emf;
    private static EntityManager em;

    public static EntityManager getEntityManager() {
        if(em == null) {
            emf = Persistence.createEntityManagerFactory("BankPU");
            em = emf.createEntityManager();
        }
        return em;
    }

    public static void closeEntityManager() {
        em.close();
        emf.close();
    }
}
