package com.comfenalco.cine.service.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comfenalco.cine.service.ProductoConfiteriaService;
import com.comfenalco.cine.dto.request.ProductoConfiteriaRequest;
import com.comfenalco.cine.dto.response.ProductoConfiteriaResponse;
import com.comfenalco.cine.exception.NoEncontradoException;
import com.comfenalco.cine.exception.ReglaNegocioException;
import com.comfenalco.cine.model.ProductoConfiteria;
import com.comfenalco.cine.model.enums.CategoriaProducto;
import com.comfenalco.cine.model.enums.EstadoProducto;
import com.comfenalco.cine.repository.ProductoConfiteriaRepository;

//Se crea la clase que hace el trabajo de ProductoConfiteriaService, aqui se hacen las acciones de la interfaz
//Con @Transactional(readOnly = true) todos los métodos son solo de lectura por defecto, 
// los que modifican datos llevan su propio @Transactional,
//  si el método termina bien se guardan todos los cambios, y si lanza un error se deshacen todos
@Service
@Transactional(readOnly = true)
public class ProductoConfiteriaServiceImpl implements ProductoConfiteriaService {

    //El repositorio es el que habla con la BD, es final porque se asigna una sola vez
    private final ProductoConfiteriaRepository productoRepository;

    //Spring le pasa el repositorio al crear la clase, así no hay que crearlo con new
    public ProductoConfiteriaServiceImpl(ProductoConfiteriaRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    //Busca el producto (o lanza error si no existe) y lo convierte en el DTO de respuesta
    @Override
    public ProductoConfiteriaResponse buscarPorId(Long id) {
        return ProductoConfiteriaResponse.de(obtenerEntidad(id));
    }

    //Si no llega categoría (null) devuelve todos los productos, si llega, solo los de esa categoría
    //Se ordena por nombre y cada producto se convierte en su DTO
    @Override
    public List<ProductoConfiteriaResponse> listarPorCategoria(CategoriaProducto categoria) {
        if (categoria == null) {
            return productoRepository.findAllByOrderByNombre().stream()
                    .map(ProductoConfiteriaResponse::de)
                    .toList();
        }
        return productoRepository.findByCategoriaOrderByNombre(categoria).stream()
                .map(ProductoConfiteriaResponse::de)
                .toList();
    }

    //Este método modifica la BD, por eso lleva @Transactional sin readOnly
    @Override
    @Transactional
    public ProductoConfiteriaResponse crear(ProductoConfiteriaRequest request) {

        //Se verifica que no exista otro producto con el mismo nombre (sin importar mayúsculas)
        if (productoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ReglaNegocioException("Ya existe un producto con ese nombre");
        }

        //Se crea el producto con los datos del request
        ProductoConfiteria producto = new ProductoConfiteria(
                request.nombre(),
                request.descripcion(),
                request.precio(),
                request.imagenUrl(),
                request.categoria());

        //Se guarda en la BD y se devuelve el producto guardado como DTO
        return ProductoConfiteriaResponse.de(productoRepository.save(producto));
    }

    //Este método también modifica la BD
    @Override
    @Transactional
    public ProductoConfiteriaResponse editar(Long id, ProductoConfiteriaRequest request) {

        //Se busca el producto que se va a editar (si no existe, lanza error)
        ProductoConfiteria producto = obtenerEntidad(id);

        //Se verifica que ningún otro producto tenga ese nombre
        // AndIdNot: el producto no se compara consigo mismo
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw new ReglaNegocioException("Ya existe otro producto con ese nombre");
        }

        //Se reemplazan los datos del producto por los del request
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setImagenUrl(request.imagenUrl());
        producto.setCategoria(request.categoria());

        return ProductoConfiteriaResponse.de(producto);   // sin save(): Hibernate guarda los cambios al terminar la transacción
    }

    //Este método también modifica la BD
    @Override
    @Transactional
    public void cambiarEstado(Long id, EstadoProducto nuevoEstado) {

        ProductoConfiteria producto = obtenerEntidad(id);

        //Si el producto ya tiene ese estado, se sale del método sin hacer nada
        if (producto.getEstado() == nuevoEstado) {
            return;   // nada que cambiar
        }

        //Solo se cambia el estado, no hace falta save(), igual que en editar
        producto.setEstado(nuevoEstado);
    }

    //Este método es de solo lectura, usa el @Transactional de la clase
    @Override
    public Map<Long, ProductoConfiteriaResponse> buscarActivosPorIds(Collection<Long> ids) {

        //Si no llegan ids, se devuelve un mapa vacío sin consultar la BD
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        // Una sola consulta que muestra todos los productos activos cuyos IDs están en la lista
        List<ProductoConfiteria> productos = productoRepository.findByIdInAndEstado(
                ids, EstadoProducto.ACTIVO);

        //Se arma el mapa: id del producto -> DTO del producto
        return productos.stream()
                .collect(Collectors.toMap(
                        ProductoConfiteria::getId,
                        ProductoConfiteriaResponse::de,
                        (a, b) -> a, // si hubiera duplicados de ID, nos quedamos con el primero
                        LinkedHashMap::new // preserva orden de inserción
                ));
    }

    //Método de ayuda, solo se usa dentro de esta clase (private)
    //Busca el producto por id y si no existe lanza NoEncontradoException
    private ProductoConfiteria obtenerEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("El producto no existe"));
    }
}