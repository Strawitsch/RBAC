public class Main {
    public static void main(String[] args) {
        System.out.println("--- Running RBAC User Validation Tests ---");
        //Тест 1. Успешное создание пользователя
        try{
            User user = User.validate("admin", "Dmitry", "dmitry@gmail.com");
            System.out.println("Created user: " + user.format());
        }
        catch(IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
        }

        //Тест 2. Неправильный формат username
        try{
            User user = User.validate("^&*())^&$^&", "Dmitry", "dmitry@gmail.com");
            System.out.println("Created user: " + user.format());
        }
        catch (IllegalArgumentException e){
            System.err.println("Caught expected error: " + e.getMessage());
        }

        //Тест 2. Пустое поле имени
        try{
            User user = User.validate("admin", "", "dmitry@gmail.com");
            System.out.println("Created user: " + user.format());
        }
        catch (IllegalArgumentException e){
            System.err.println("Caught expected error: " + e.getMessage());
        }

        //Тест 3. Пустой email
        try{
            User user = User.validate("admin", "Dmitry", "");
            System.out.println("Created user: " + user.format());
        }
        catch (IllegalArgumentException e){
            System.err.println("Caught expected error: " + e.getMessage());
        }

        //Тест 4. Неправильный формат email
        try{
            User user = User.validate("admin", "Dmitry", "dmitry@gmailcom");
            System.out.println("Created user: " + user.format());
        }
        catch(IllegalArgumentException e){
            System.err.println("Error: " + e.getMessage());
        }
    }
}