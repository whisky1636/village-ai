import request from './request'

/**
 * 活动管理API
 */

// 分页查询活动列表
export function getActivityPage(params) {
  return request({
    url: '/activities/page',
    method: 'get',
    params
  })
}

// 获取活动详情
export function getActivityDetail(id) {
  return request({
    url: `/activities/${id}`,
    method: 'get'
  })
}

// 创建活动
export function createActivity(data) {
  return request({
    url: '/activities',
    method: 'post',
    data
  })
}

// 更新活动
export function updateActivity(id, data) {
  return request({
    url: `/activities/${id}`,
    method: 'put',
    data
  })
}

// 删除活动
export function deleteActivity(id) {
  return request({
    url: `/activities/${id}`,
    method: 'delete'
  })
}

// 发布活动
export function publishActivity(id) {
  return request({
    url: `/activities/${id}/publish`,
    method: 'post'
  })
}

// 获取我的活动报名记录
export function getMyActivityRegistrations(params) {
  return request({
    url: '/activities/my-registrations',
    method: 'get',
    params
  })
}

// 取消活动报名
export function cancelActivityRegistration(id) {
  return request({
    url: `/activities/registrations/${id}/cancel`,
    method: 'post'
  })
}

// 取消活动
export function cancelActivity(id) {
  return request({
    url: `/activities/${id}/cancel`,
    method: 'post'
  })
}

/**
 * 活动报名API
 */

// 分页查询报名列表
export function getRegistrationPage(params) {
  return request({
    url: '/activity-registrations/page',
    method: 'get',
    params
  })
}

// 获取用户的报名列表
export function getMyRegistrations(params) {
  return request({
    url: '/activity-registrations/my-registrations',
    method: 'get',
    params
  })
}

// 检查是否已报名
export function checkRegistration(activityId) {
  return request({
    url: `/activity-registrations/check/${activityId}`,
    method: 'get'
  })
}

// 创建报名
export function createRegistration(data) {
  return request({
    url: '/activity-registrations',
    method: 'post',
    data
  })
}

// 取消报名
export function cancelRegistration(id) {
  return request({
    url: `/activity-registrations/${id}/cancel`,
    method: 'post'
  })
}

// 审核报名
export function auditRegistration(id, data) {
  return request({
    url: `/activity-registrations/${id}/audit`,
    method: 'post',
    data
  })
}

// 支付报名费
export function payRegistrationFee(id, data) {
  return request({
    url: `/activity-registrations/${id}/pay`,
    method: 'post',
    data
  })
}

// 签到
export function checkIn(id, data) {
  return request({
    url: `/activity-registrations/${id}/check-in`,
    method: 'post',
    data
  })
}






