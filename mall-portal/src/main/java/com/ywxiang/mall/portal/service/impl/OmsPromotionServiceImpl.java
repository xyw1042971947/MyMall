package com.ywxiang.mall.portal.service.impl;

import com.ywxiang.mall.model.OmsCartItem;
import com.ywxiang.mall.model.PmsProductFullReduction;
import com.ywxiang.mall.model.PmsProductLadder;
import com.ywxiang.mall.model.PmsSkuStock;
import com.ywxiang.mall.portal.dao.PortalProductDao;
import com.ywxiang.mall.portal.domain.CartPromotionItem;
import com.ywxiang.mall.portal.domain.PromotionProduct;
import com.ywxiang.mall.portal.service.OmsPromotionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 促销管理Service实现类
 *
 * @author ywxiang
 * @date 2020/12/7 下午9:13
 */
@Service
public class OmsPromotionServiceImpl implements OmsPromotionService {
    @Autowired
    private PortalProductDao portalProductDao;

    @Override
    public List<CartPromotionItem> calcCartPromotion(List<OmsCartItem> cartItemList) {
        // 1.先根据productId对CartItem进行分组，以spu为单位进行计算优惠
        Map<Long, List<OmsCartItem>> productCartMap =
                cartItemList.stream().collect(Collectors.groupingBy(OmsCartItem::getProductId));
        // 2.查询所有商品的优惠相关信息
        List<Long> productIdList = cartItemList.stream().map(OmsCartItem::getProductId).collect(Collectors.toList());
        List<PromotionProduct> promotionProductList = portalProductDao.getPromotionProductList(productIdList);
        // 3.根据商品促销类型计算商品促销优惠价格
        List<CartPromotionItem> cartPromotionItemList = new ArrayList<>();
        for (Map.Entry<Long, List<OmsCartItem>> entry : productCartMap.entrySet()) {
            Long productId = entry.getKey();
            // 商品的促销信息——打折——促销——满减
            PromotionProduct promotionProduct = getPromotionProductById(productId, promotionProductList);
            List<OmsCartItem> items = entry.getValue();
            if (promotionProduct != null) {
                Integer promotionType = promotionProduct.getPromotionType();
                if (promotionType == 1) {
                    // 单品促销
                    for (OmsCartItem item : items) {
                        CartPromotionItem cartPromotionItem = new CartPromotionItem();
                        BeanUtils.copyProperties(item, cartPromotionItem);
                        cartPromotionItem.setPromotionMessage("单品促销");
                        // 商品原价-促销价
                        PmsSkuStock skuStock = getOriginalPrice(promotionProduct, item.getProductSkuId());
                        assert skuStock != null;
                        BigDecimal originalPrice = skuStock.getPrice();
                        // 单品促销使用原价
                        cartPromotionItem.setPrice(originalPrice);
                        // 促销活动减去的的价格 = 商品的价格 - 促销活动的价格
                        cartPromotionItem.setReduceAmount(originalPrice.subtract(skuStock.getPromotionPrice()));
                        // 真实库存 = 库存 - 锁定库存
                        cartPromotionItem.setRealStock(skuStock.getStock() - skuStock.getLockStock());
                        // 赠送的积分
                        cartPromotionItem.setIntegration(promotionProduct.getGiftPoint());
                        // 赠送的成长值
                        cartPromotionItem.setGrowth(promotionProduct.getGiftGrowth());
                        cartPromotionItemList.add(cartPromotionItem);
                    }
                } else if (promotionType == 3) {
                    // 打折优惠
                    int count = getCartItemCount(items);
                    PmsProductLadder ladder = getProductLadder(count, promotionProduct.getProductLadderList());
                    if (ladder != null) {
                        for (OmsCartItem item : items) {
                            CartPromotionItem cartPromotionItem = new CartPromotionItem();
                            BeanUtils.copyProperties(item, cartPromotionItem);
                            String message = getLadderPromotionMessage(ladder);
                            cartPromotionItem.setPromotionMessage(message);
                            // 商品原价 - 折扣*商品原价
                            PmsSkuStock skuStock = getOriginalPrice(promotionProduct, item.getProductSkuId());
                            assert skuStock != null;
                            BigDecimal originalPrice = skuStock.getPrice();
                            BigDecimal reduceAmount = originalPrice.subtract(ladder.getDiscount().multiply(originalPrice));
                            cartPromotionItem.setReduceAmount(reduceAmount);
                            cartPromotionItem.setRealStock(skuStock.getStock() - skuStock.getLockStock());
                            cartPromotionItem.setIntegration(promotionProduct.getGiftPoint());
                            cartPromotionItem.setGrowth(promotionProduct.getGiftGrowth());
                            cartPromotionItemList.add(cartPromotionItem);
                        }
                    } else {
                        handleNoReduce(cartPromotionItemList, items, promotionProduct);
                    }
                } else if (promotionType == 4) {
                    //满减
                    BigDecimal totalAmount = getCartItemAmount(items, promotionProductList);
                    // 获取满减信息
                    PmsProductFullReduction fullReduction = getProductFullReduction(totalAmount, promotionProduct.getProductFullReductionList());
                    if (fullReduction != null) {
                        for (OmsCartItem item : items) {
                            CartPromotionItem cartPromotionItem = new CartPromotionItem();
                            BeanUtils.copyProperties(item, cartPromotionItem);
                            String message = getFullReductionPromotionMessage(fullReduction);
                            cartPromotionItem.setPromotionMessage(message);
                            //(商品原价/总价)*满减金额
                            PmsSkuStock skuStock = getOriginalPrice(promotionProduct, item.getProductSkuId());
                            BigDecimal originalPrice = skuStock.getPrice();
                            BigDecimal reduceAmount = originalPrice.divide(totalAmount, RoundingMode.HALF_EVEN).multiply(fullReduction.getReducePrice());
                            cartPromotionItem.setReduceAmount(reduceAmount);
                            cartPromotionItem.setRealStock(skuStock.getStock() - skuStock.getLockStock());
                            cartPromotionItem.setIntegration(promotionProduct.getGiftPoint());
                            cartPromotionItem.setGrowth(promotionProduct.getGiftGrowth());
                            cartPromotionItemList.add(cartPromotionItem);
                        }
                    } else {
                        handleNoReduce(cartPromotionItemList, items, promotionProduct);
                    }
                } else {
                    handleNoReduce(cartPromotionItemList, items, promotionProduct);
                }
            }

        }
        return null;
    }

    /**
     * 根据商品id获取商品促销信息
     *
     * @param productId
     * @param promotionProductList
     * @return
     */
    private PromotionProduct getPromotionProductById(Long productId, List<PromotionProduct> promotionProductList) {
        for (PromotionProduct promotionProduct : promotionProductList) {
            if (productId.equals(promotionProduct.getId())) {
                return promotionProduct;
            }
        }
        return null;
    }

    /**
     * 获取购物车指定商品的数量
     *
     * @param items
     * @return
     */
    private int getCartItemCount(List<OmsCartItem> items) {
        int count = 0;
        for (OmsCartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    /**
     * 获取商品原定价
     *
     * @param promotionProduct
     * @param productSkuId
     * @return
     */
    private PmsSkuStock getOriginalPrice(PromotionProduct promotionProduct, Long productSkuId) {
        for (PmsSkuStock skuStock : promotionProduct.getSkuStockList()) {
            if (productSkuId.equals(skuStock.getId())) {
                return skuStock;
            }
        }
        return null;
    }

    /**
     * 根据购买商品数量获取满足条件的打折优惠策略
     *
     * @param count
     * @param productLadderList
     * @return
     */
    private PmsProductLadder getProductLadder(int count, List<PmsProductLadder> productLadderList) {
        // 数量从大到小排序
        productLadderList.sort((o1, o2) -> o2.getCount() - o1.getCount());
        for (PmsProductLadder productLadder : productLadderList) {
            if (count >= productLadder.getCount()) {
                return productLadder;
            }
        }
        return null;
    }

    /**
     * 获取打折优惠的促销信息
     *
     * @param ladder
     * @return
     */
    private String getLadderPromotionMessage(PmsProductLadder ladder) {
        StringBuilder sb = new StringBuilder();
        sb.append("打折优惠：");
        sb.append("满");
        sb.append(ladder.getCount());
        sb.append("件，");
        sb.append("打");
        sb.append(ladder.getDiscount().multiply(new BigDecimal(10)));
        sb.append("折");
        return sb.toString();
    }

    /**
     * 对没满足条件的商品进行处理
     *
     * @param cartPromotionItemList
     * @param itemList
     * @param promotionProduct
     */
    private void handleNoReduce(List<CartPromotionItem> cartPromotionItemList, List<OmsCartItem> itemList, PromotionProduct promotionProduct) {
        for (OmsCartItem item : itemList) {
            CartPromotionItem cartPromotionItem = new CartPromotionItem();
            BeanUtils.copyProperties(item, cartPromotionItem);
            cartPromotionItem.setPromotionMessage("无优惠");
            cartPromotionItem.setReduceAmount(new BigDecimal(0));
            PmsSkuStock skuStock = getOriginalPrice(promotionProduct, item.getProductSkuId());
            if (skuStock != null) {
                cartPromotionItem.setRealStock(skuStock.getStock() - skuStock.getLockStock());
            }
            cartPromotionItem.setIntegration(promotionProduct.getGiftPoint());
            cartPromotionItem.setGrowth(promotionProduct.getGiftGrowth());
            cartPromotionItemList.add(cartPromotionItem);
        }
    }


    /**
     * 获取购物车中指定商品的总价
     *
     * @param itemList
     * @param promotionProductList
     * @return
     */
    private BigDecimal getCartItemAmount(List<OmsCartItem> itemList, List<PromotionProduct> promotionProductList) {
        BigDecimal amount = new BigDecimal(0);
        for (OmsCartItem item : itemList) {
            //计算出商品原价
            PromotionProduct promotionProduct = getPromotionProductById(item.getProductId(), promotionProductList);
            PmsSkuStock skuStock = getOriginalPrice(promotionProduct, item.getProductSkuId());
            amount = amount.add(skuStock.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        return amount;
    }

    /**
     * 获取商品的满减信息
     *
     * @param totalAmount       总价
     * @param fullReductionList 商品满减信息
     * @return
     */
    private PmsProductFullReduction getProductFullReduction(BigDecimal totalAmount, List<PmsProductFullReduction> fullReductionList) {
        //按条件从高到低排序
        fullReductionList.sort((o1, o2) -> o2.getFullPrice().subtract(o1.getFullPrice()).intValue());
        for (PmsProductFullReduction fullReduction : fullReductionList) {
            if (totalAmount.subtract(fullReduction.getFullPrice()).intValue() >= 0) {
                return fullReduction;
            }
        }
        return null;
    }

    /**
     * 满减消息
     *
     * @param fullReduction
     * @return
     */
    private String getFullReductionPromotionMessage(PmsProductFullReduction fullReduction) {
        StringBuilder sb = new StringBuilder();
        sb.append("满减优惠：");
        sb.append("满");
        sb.append(fullReduction.getFullPrice());
        sb.append("元，");
        sb.append("减");
        sb.append(fullReduction.getReducePrice());
        sb.append("元");
        return sb.toString();
    }

}
