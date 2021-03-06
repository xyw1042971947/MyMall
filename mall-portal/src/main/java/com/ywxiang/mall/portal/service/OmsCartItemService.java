package com.ywxiang.mall.portal.service;

import com.ywxiang.mall.model.OmsCartItem;
import com.ywxiang.mall.portal.domain.CartProduct;
import com.ywxiang.mall.portal.domain.CartPromotionItem;

import java.util.List;

/**
 * 购物车管理Service
 *
 * @author ywxiang
 * @date 2020/12/7 下午8:18
 */
public interface OmsCartItemService {

    /**
     * 查询购物车有无该商品，有增加数量，没有添加到购物车
     * @param cartItem
     * @return
     */
    int add (OmsCartItem cartItem);

    /**
     * 根据会员编号获取购物车列表
     *
     * @param memberId
     * @return
     */
    List<OmsCartItem> list(Long memberId);

    /**
     * 获取包含促销活动信息的购物车列表
     *
     * @param memberId
     * @param cartIds
     * @return
     */
    List<CartPromotionItem> listPromotion(Long memberId, List<Long> cartIds);

    /**
     * 修改购物车中某个商品的数量
     *
     * @param id
     * @param memberId
     * @param quantity
     * @return
     */
    int updateQuantity(Long id, Long memberId, Integer quantity);

    /**
     * 批量删除购物车中的商品
     *
     * @param memberId
     * @param ids
     * @return
     */
    int delete(Long memberId, List<Long> ids);

    /**
     * 获取购物车用于选择商品规格的商品信息
     *
     * @param productId
     * @return
     */
    CartProduct getCartProduct(Long productId);

    /**
     * 修改购物车中的商品规格
     *
     * @param cartItem
     * @return
     */
    int updateAttr(OmsCartItem cartItem);

    /***
     * 清空购物车
     * @param memberId
     * @return
     */
    int clear(Long memberId);
}
