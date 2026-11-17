package com.comfenalco.cine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.comfenalco.cine.model.Usuario;
import com.comfenalco.cine.model.enums.EstadoUsuario;
import com.comfenalco.cine.model.enums.RolUsuario;
import com.comfenalco.cine.repository.UsuarioRepository;

@Configuration
public class SeguridadConfig implements WebMvcConfigurer {

    // Asocia la ruta /login con la vista login.html, permite mostrar la 
    // página de inicio de sesión sin necesidad de crear un método en un Controller.
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }

    // Protege las contraseñas mediante el algoritmo BCrypt para evitar almacenarlas como texto plano.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security llama a este método automáticamente cuando alguien intenta
    // iniciar sesión en el formulario de login.
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return nombreDeUsuario -> {
            // 1. Buscamos el usuario en la base de datos verificando que esté ACTIVO
            Usuario usuarioEncontrado = usuarioRepository
                    .findByUsernameAndEstado(nombreDeUsuario, EstadoUsuario.ACTIVO)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Credenciales inválidas: usuario no encontrado o inactivo"));

            // 2. Regla de negocio: Solo los administradores pueden iniciar sesión.
            //    Los taquilleros atienden en mostrador y no requieren cuenta de login.
            if (usuarioEncontrado.getRol() != RolUsuario.ADMINISTRADOR) {
                throw new UsernameNotFoundException(
                        "Acceso denegado: solo el administrador tiene permiso para ingresar al sistema");
            }

            // 3. Le entregamos a Spring Security los datos del administrador autenticado
            return User.builder()
                    .username(usuarioEncontrado.getUsername())
                    .password(usuarioEncontrado.getContrasenaHash())
                    .roles("ADMINISTRADOR")
                    .build();
        };
    }

    // Configuración de rutas y permisos (Qué está protegido y qué es público)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizaciones -> autorizaciones
                        // Solo el ADMINISTRADOR puede ingresar a Administración, Reportes y Facturas
                        .requestMatchers("/administracion", "/administracion/**",
                                         "/reportes", "/reportes/**")
                        .hasRole("ADMINISTRADOR")

                        // Archivos de diseño (CSS, JS, imágenes), pantalla de login y errores son públicos
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico", "/login", "/error")
                        .permitAll()

                        // Todo lo demás (Inicio, Cartelera, Funciones, Boletos, Cafetería) es público
                        .anyRequest()
                        .permitAll())

                // Formulario de login personalizado con la plantilla login.html
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }
}