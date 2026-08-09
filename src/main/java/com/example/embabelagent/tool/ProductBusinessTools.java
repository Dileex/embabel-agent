package com.example.embabelagent.tool;

import com.embabel.agent.api.annotation.LlmTool;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductBusinessTools {

    private static final String DEMO_TENANT = "tenant-demo";

    private final Map<String, ProductInfo> products = Map.of(
            "P-1001",
            new ProductInfo(
                    "P-1001",
                    "轻量通勤双肩包",
                    "ON_SALE",
                    List.of(
                            "重量约620克",
                            "容量18升",
                            "支持放入14英寸笔记本",
                            "日常防泼水")));

    private final Map<String, InventoryPrice> inventoryPrices =
            Map.of(
                    "P-1001",
                    new InventoryPrice(
                            "P-1001",
                            86,
                            new BigDecimal("199.00"),
                            "CNY"));

    private final Map<String, StoreInfo> stores = Map.of(
            "S-2001",
            new StoreInfo(
                    "S-2001",
                    "通勤研究所",
                    true,
                    Set.of("抖音", "小红书")));

    private final Map<String, PlatformSpec> platformSpecs = Map.of(
            "抖音",
            new PlatformSpec(
                    "抖音",
                    300,
                    "9:16",
                    List.of(
                            "商品名称",
                            "商品卖点",
                            "价格信息",
                            "素材来源")));

    public ProductQueryTools forTenant(String tenantId) {
        validateTenantAccess(tenantId);
        /*
         * 不复用单例工具对象：calledTools是一次请求的执行记录，
         * 独立对象可以避免并发请求把彼此调用过的工具混在一起。
         */
        return new ProductQueryTools(
                tenantId,
                products,
                inventoryPrices,
                stores,
                platformSpecs);
    }

    public void validateTenantAccess(String tenantId) {
        if (!DEMO_TENANT.equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "当前租户无权访问示例商品数据");
        }
    }

    public static final class ProductQueryTools {

        private final String tenantId;

        private final Map<String, ProductInfo> products;

        private final Map<String, InventoryPrice> inventoryPrices;

        private final Map<String, StoreInfo> stores;

        private final Map<String, PlatformSpec> platformSpecs;

        /*
         * 记录本次请求成功返回数据的工具。它用于返回给调用方观察执行过程，
         * 不是让模型填写的响应字段。
         */
        private final List<String> calledTools = new ArrayList<>();

        private ProductQueryTools(
                String tenantId,
                Map<String, ProductInfo> products,
                Map<String, InventoryPrice> inventoryPrices,
                Map<String, StoreInfo> stores,
                Map<String, PlatformSpec> platformSpecs) {
            this.tenantId = tenantId;
            this.products = products;
            this.inventoryPrices = inventoryPrices;
            this.stores = stores;
            this.platformSpecs = platformSpecs;
        }

        @LlmTool(
                name = "query_product",
                description = "按商品ID查询商品名称、状态和已确认卖点")
        public ProductInfo queryProduct(
                @LlmTool.Param(
                        description = "业务系统中的商品ID")
                String productId) {
            verifyTenant();
            ProductInfo result = requireValue(
                    products,
                    productId,
                    "商品不存在：" + productId);
            calledTools.add("query_product");
            return result;
        }

        @LlmTool(
                name = "query_inventory_price",
                description = "按商品ID查询可售库存和当前销售价格")
        public InventoryPrice queryInventoryPrice(
                @LlmTool.Param(
                        description = "业务系统中的商品ID")
                String productId) {
            verifyTenant();
            InventoryPrice result = requireValue(
                    inventoryPrices,
                    productId,
                    "没有找到库存和价格：" + productId);
            calledTools.add("query_inventory_price");
            return result;
        }

        @LlmTool(
                name = "query_store",
                description = "按店铺ID查询店铺状态和允许发布的平台")
        public StoreInfo queryStore(
                @LlmTool.Param(
                        description = "业务系统中的店铺ID")
                String storeId) {
            verifyTenant();
            StoreInfo result = requireValue(
                    stores,
                    storeId,
                    "店铺不存在：" + storeId);
            calledTools.add("query_store");
            return result;
        }

        @LlmTool(
                name = "query_platform_spec",
                description = "查询发布平台的视频时长、画面比例和必填信息")
        public PlatformSpec queryPlatformSpec(
                @LlmTool.Param(
                        description = "发布平台中文名称")
                String platform) {
            verifyTenant();
            PlatformSpec result = requireValue(
                    platformSpecs,
                    platform,
                    "没有找到平台规则：" + platform);
            calledTools.add("query_platform_spec");
            return result;
        }

        public List<String> calledTools() {
            // 同一工具被模型重复调用时，响应中只保留一次工具名，并保持首次调用顺序。
            return List.copyOf(
                    new LinkedHashSet<>(calledTools));
        }

        private void verifyTenant() {
            if (!DEMO_TENANT.equals(tenantId)) {
                throw new IllegalArgumentException(
                        "当前租户无权访问示例商品数据");
            }
        }

        private static <T> T requireValue(
                Map<String, T> values,
                String key,
                String message) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "查询参数不能为空");
            }
            T value = values.get(key.strip());
            if (value == null) {
                throw new IllegalArgumentException(message);
            }
            return value;
        }
    }

    public record ProductInfo(
            String productId,
            String productName,
            String status,
            List<String> confirmedSellingPoints) {
    }

    public record InventoryPrice(
            String productId,
            int availableStock,
            BigDecimal salePrice,
            String currency) {
    }

    public record StoreInfo(
            String storeId,
            String storeName,
            boolean enabled,
            Set<String> allowedPlatforms) {
    }

    public record PlatformSpec(
            String platform,
            int maxVideoDurationSeconds,
            String preferredAspectRatio,
            List<String> requiredInformation) {
    }
}
