package com.sriundee.preorder.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import com.sriundee.preorder.bean.ProductBean;
import com.sriundee.preorder.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock MenuController menus;
    @Mock ProductRepository products;
    @Mock ArtistController artists;
    @Mock TypeController types;
    @Mock ProductStatusController statuses;
    @InjectMocks ProductController controller;

    @Test void productListKeepsAngleBracketsAsVisibleTextInsteadOfHtmlTags() {
        var product=mock(ProductBean.class);
        when(product.getID_product()).thenReturn(1);
        when(product.getID_pro_status()).thenReturn(1);
        when(product.getp_name()).thenReturn("NMIXX 2027 SEASON’S GREETINGS <MIXX ON THE ROAD> & SPECIAL");
        when(products.getDataAll()).thenReturn(List.of(product));
        var model=new ExtendedModelMap();
        assertThat(controller.index(model)).isEqualTo("product/product");
        assertThat(model.get("mainProduct").toString())
                .contains("<td>NMIXX 2027 SEASON’S GREETINGS &lt;MIXX ON THE ROAD&gt; &amp; SPECIAL</td>")
                .contains("onclick='ProductPriceTable.open(1)'")
                .contains("aria-label='ดูตารางราคา'")
                .doesNotContain("<MIXX ON THE ROAD>");
        verify(products,never()).save(any());
    }

    @Test void markupInProductNameCannotCreateAnElementInTheProductList() {
        var product=mock(ProductBean.class);
        when(product.getID_product()).thenReturn(2);
        when(product.getID_pro_status()).thenReturn(1);
        when(product.getp_name()).thenReturn("<img src=x onerror='alert(1)'>");
        when(products.getDataAll()).thenReturn(List.of(product));
        var model=new ExtendedModelMap();controller.index(model);
        assertThat(model.get("mainProduct").toString())
                .contains("<td>&lt;img src=x onerror=&#39;alert(1)&#39;&gt;</td>")
                .doesNotContain("<img src=x");
    }
}
