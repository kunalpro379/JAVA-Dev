package annotations;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

// Custom annotation for validation
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface Validate {
    String message() default "Validation failed";
    int min() default 0;
    int max() default Integer.MAX_VALUE;
    boolean required() default false;
}

// Custom annotation for method timing
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface TimeExecution {
    String description() default "";
}

// Custom annotation for API documentation
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface API {
    String version();
    String author();
    String[] endpoints();
}

@API(
    version = "1.0",
    author = "John Doe",
    endpoints = {"/users", "/users/{id}"}
)
class UserService {
    @Validate(required = true)
    private String username;

    @Validate(min = 18, max = 100, message = "Age must be between 18 and 100")
    private int age;

    @Validate(required = true)
    private String email;

    public UserService(String username, int age, String email) {
        this.username = username;
        this.age = age;
        this.email = email;
    }

    @TimeExecution(description = "Process user data")
    public void processUser() throws InterruptedException {
        Thread.sleep(100); // Simulate processing
        System.out.println("Processing user: " + username);
    }
}

// Aspect-like class to handle method timing
class ExecutionTimer {
    public static Object timeMethod(Method method, Object instance, Object... args) 
            throws Exception {
        TimeExecution annotation = method.getAnnotation(TimeExecution.class);
        if (annotation != null) {
            String description = annotation.description();
            long start = System.nanoTime();
            
            Object result = method.invoke(instance, args);
            
            long end = System.nanoTime();
            System.out.printf("%s took %d ms%n", 
                description.isEmpty() ? method.getName() : description,
                (end - start) / 1_000_000);
            
            return result;
        }
        return method.invoke(instance, args);
    }
}

// Validator using reflection
class Validator {
    public static List<String> validate(Object object) {
        List<String> validationErrors = new ArrayList<>();
        Class<?> clazz = object.getClass();

        // Validate fields
        for (Field field : clazz.getDeclaredFields()) {
            Validate annotation = field.getAnnotation(Validate.class);
            if (annotation != null) {
                field.setAccessible(true);
                try {
                    Object value = field.get(object);
                    
                    // Required check
                    if (annotation.required() && value == null) {
                        validationErrors.add(field.getName() + " is required");
                        continue;
                    }

                    // Numeric validations
                    if (value instanceof Number) {
                        int numericValue = ((Number) value).intValue();
                        if (numericValue < annotation.min() || numericValue > annotation.max()) {
                            validationErrors.add(annotation.message());
                        }
                    }
                } catch (IllegalAccessException e) {
                    validationErrors.add("Could not access " + field.getName());
                }
            }
        }
        return validationErrors;
    }
}

public class ReflectionDemo {
    public static void main(String[] args) {
        try {
            // Create instance and validate
            UserService user = new UserService("john_doe", 25, "john@example.com");
            List<String> validationErrors = Validator.validate(user);
            
            if (!validationErrors.isEmpty()) {
                System.out.println("Validation errors:");
                validationErrors.forEach(System.out::println);
            } else {
                System.out.println("Validation passed");
            }

            // Demonstrate method timing using reflection
            Method processMethod = UserService.class.getMethod("processUser");
            ExecutionTimer.timeMethod(processMethod, user);

            // Demonstrate API annotation reflection
            API apiAnnotation = UserService.class.getAnnotation(API.class);
            if (apiAnnotation != null) {
                System.out.println("\nAPI Documentation:");
                System.out.println("Version: " + apiAnnotation.version());
                System.out.println("Author: " + apiAnnotation.author());
                System.out.println("Endpoints: " + String.join(", ", apiAnnotation.endpoints()));
            }

            // Demonstrate general reflection capabilities
            System.out.println("\nClass structure analysis:");
            analyzeClass(UserService.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void analyzeClass(Class<?> clazz) {
        System.out.println("Class name: " + clazz.getName());

        // Fields
        System.out.println("\nFields:");
        for (Field field : clazz.getDeclaredFields()) {
            System.out.printf("- %s %s%n", 
                field.getType().getSimpleName(), 
                field.getName());
        }

        // Methods
        System.out.println("\nMethods:");
        for (Method method : clazz.getDeclaredMethods()) {
            System.out.printf("- %s %s(%s)%n",
                method.getReturnType().getSimpleName(),
                method.getName(),
                Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("")
            );
        }

        // Annotations
        System.out.println("\nAnnotations:");
        for (Annotation annotation : clazz.getAnnotations()) {
            System.out.println("- " + annotation);
        }
    }
}