package org.example.service;


import org.example.entity.IgUser;

import java.util.Optional;

public interface IgUserService extends BaseService<IgUser> {
    Optional<IgUser> findUserByIgPk(long igPk);

    /**
     * 透過IG用戶名查詢用戶
     *
     * @param igUserName IG用戶名
     * @return 用戶
     */
    Optional<IgUser> findUserByIgUserName(String igUserName);

    /**
     * 保存或更新用戶
     *
     * @param newUser 用戶
     * @return 用戶
     */
    IgUser saveOrUpdateIgUser(IgUser newUser);
}