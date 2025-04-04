package com.marcosoft.storageSoftware.service.impl;

import com.marcosoft.storageSoftware.model.Product;
import com.marcosoft.storageSoftware.repository.ProductRepository;
import com.marcosoft.storageSoftware.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return (List<Product>) productRepository.findAll();
    }

    @Override
    public void deleteByProductName(String productName) {
         productRepository.deleteByProductName(productName);
    }

    @Override
    public Product getByProductName(String name) {
        return productRepository.findByProductName(name);
    }

    @Override
    public void updateQuantityInStorageByProductName(Integer stock, String name) {
        productRepository.updateQuantityInStorageByProductName(stock, name);
    }

    public boolean productExists(String productName) {
        return productRepository.findByProductName(productName) != null;
    }

    public Product createOrUpdateProduct(Product product) {
        Product existingProduct = productRepository.findByProductName(product.getProductName());
        if (existingProduct != null) {
            // Actualizar producto existente
            existingProduct.setCategoryName(product.getCategoryName());
            existingProduct.setQuantityInStorage(product.getQuantityInStorage());
            existingProduct.setPricePerUnit(product.getPricePerUnit());
            existingProduct.setCurrencyName(product.getCurrencyName());
            existingProduct.setStoredIn(product.getStoredIn());
            return productRepository.save(existingProduct);
        } else {
            // Crear nuevo producto
            return productRepository.save(product);
        }
    }
}
