package org.example.service.impl;

import org.example.dao.FollowersDao;
import org.example.entity.Followers;
import org.example.entity.IgUser;
import org.example.exception.ApiException;
import org.example.exception.SysCode;
import org.example.service.FollowersService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Eric.Lee
 * Date:2024/2/18
 */
@Service("followersService")
public class FollowersServiceImpl implements FollowersService {
    private final FollowersDao followersDao;

    public FollowersServiceImpl(FollowersDao followersDao) {
        this.followersDao = followersDao;
    }


    @Override
    public void batchInsertFollowers(List<Followers> followersList) {
        followersDao.batchInsertOrUpdate(followersList);
    }

    @Override
    public int countFollowersByIgUserName(IgUser igUser) {
        return followersDao.countByIgUser(igUser);
    }

    @Override
    public Optional<Followers> save(Followers target) {
        return Optional.of(followersDao.save(target));
    }

    @Override
    public Optional<Followers> findById(Integer id) {
        return followersDao.findById(id);
    }

    @Override
    public List<Followers> findAll() {
        return followersDao.findAll();
    }

    @Override
    public void deleteOldFollowersDataByIgUser(IgUser igUser) {
        followersDao.deleteByIgUser(igUser);
    }

    @Override
    public List<Followers> findByIgUser(IgUser igUser) {
        List<Followers> followersList = followersDao.findByIgUser(igUser);
        if (followersList.isEmpty()) {
            throw new ApiException(SysCode.FOLLOWERS_OR_MEDIA_AMOUNT_IS_ZERO);
        }
        return followersList;
    }
}
