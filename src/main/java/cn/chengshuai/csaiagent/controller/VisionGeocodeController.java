package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 逆地理编码接口：坐标 → 城市/区名，供前端定位按钮使用。
 */
@RestController
@RequestMapping("/vision/geocode")
@Slf4j
public class VisionGeocodeController {

    @Value("${local-api-keys.amap.maps-api-key:}")
    private String amapKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 逆地理编码，返回格式化地址（省市区）。
     *
     * @param lat 纬度
     * @param lng 经度
     */
    @GetMapping("/reverse")
    public ApiResponse<String> reverse(@RequestParam double lat, @RequestParam double lng) {
        if (amapKey == null || amapKey.isBlank()) {
            return ApiResponse.error(ResultCode.DEPENDENCY_UNAVAILABLE, "高德 key 未配置");
        }
        try {
            String url = "https://restapi.amap.com/v3/geocode/regeo?key=" + amapKey
                    + "&location=" + lng + "," + lat
                    + "&extensions=base&output=json";
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null || !"1".equals(String.valueOf(resp.get("status")))) {
                return ApiResponse.error(ResultCode.INTERNAL_ERROR, "逆地理编码失败");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> regeocode = (Map<String, Object>) resp.get("regeocode");
            if (regeocode == null) {
                return ApiResponse.error(ResultCode.NOT_FOUND, "未返回地址信息");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> ac = (Map<String, Object>) regeocode.get("addressComponent");
            if (ac == null) {
                String formatted = (String) regeocode.get("formatted_address");
                return ApiResponse.success(formatted != null ? formatted : "");
            }
            String province = toStr(ac.get("province"));
            String city = toStr(ac.get("city"));
            String district = toStr(ac.get("district"));
            String cityPart = (city == null || city.isBlank()) ? province : city;
            String location = district != null && !district.isBlank()
                    ? cityPart + district
                    : cityPart;
            return ApiResponse.success(location);
        } catch (Exception e) {
            log.warn("逆地理编码异常: lat={}, lng={}", lat, lng, e);
            return ApiResponse.error(ResultCode.INTERNAL_ERROR, "定位解析失败：" + e.getMessage());
        }
    }

    private String toStr(Object obj) {
        if (obj == null) return "";
        String s = String.valueOf(obj);
        return "[]".equals(s) ? "" : s;
    }
}
