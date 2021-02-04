package Server;

import java.util.ArrayList;
import java.util.List;

public class BaseAuthService implements AuthService {

    private final List<Entry> entries;

    public BaseAuthService() {
        entries = new ArrayList<>();
        entries.add(new Entry("Ivanov", "1", "vano"));
        entries.add(new Entry("Petrov", "2", "petro"));
        entries.add(new Entry("Sidorov", "3", "sidor"));
    }

    private class Entry {
        String login;
        String password;
        String nick;

        public Entry(String login, String password, String nick) {
            this.login = login;
            this.password = password;
            this.nick = nick;
        }
    }

    @Override
    public void start() {
        System.out.println("Сервис авторизации запущен");
    }

    @Override
    public void stop() {
        System.out.println("Сервис авторизации остановлен");
    }

    @Override
    public String getNickByLoginAndPass(String login, String password) {
        for (Entry entry : entries) {
            if(login.equals(entry.login) && password.equals(entry.password)) {
                return entry.nick;
            }
        }
        return null;
    }
}
