import numpy as np
import matplotlib.pyplot as plt

# Given data
# (Assuming 'predictionData_deploymentCVSS.txt' contains data in the format: 'deployment_frequency = X, CVSS = Y')
with open('predictionData_deploymentCVSS.txt', 'r') as file:
    lines = file.readlines()
deployment_frequency = []
CVSS = []

for line in lines:
    parts = line.split(', ')
    freq_part = parts[0].split(' = ')
    score_part = parts[1].split(' = ')
    
    deployment_frequency.append(int(freq_part[1]))
    CVSS.append(float(score_part[1]))

# Calculate Z-score
def z_score(data):
    mean = np.mean(data)
    std_dev = np.std(data)
    z_scores = [(x - mean) / std_dev for x in data]
    return z_scores

# Set threshold (adjust as needed)
threshold = 2
# Define functions for WMA and forecasting
def calculate_ewma_weights(alpha, num_points):
    weights = [(1 - alpha)**t for t in reversed(range(num_points))]
    total_weight = sum(weights)
    normalized_weights = [w / total_weight for w in weights]
    return np.array(normalized_weights)

def wma(data, weights):
    n = len(data)
    smoothed_data = np.zeros(n)
    for i in range(n):
        smoothed_data[i] = np.sum(data[max(0, i-n+1):i+1] * weights[max(n-i-1, 0):])
    smoothed_data[smoothed_data < 0] = 0
    return smoothed_data / np.sum(weights[:n])

def forecast_wma(data, weights, num_forecast):
    n = len(data)
    smoothed_data = np.zeros(n + num_forecast)
    smoothed_data[:n] = wma(data, weights)
    for i in range(n, n + num_forecast):
        smoothed_data[i] = np.sum(smoothed_data[i-n:i] * weights[-min(i, n):])
    return smoothed_data[n:]


# Set threshold (adjust as needed)
threshold = 2
alpha = 0.3
num_forecast = 3

# Calculate Z-scores for deployment_frequency and CVSS
z_scores_deployment = z_score(deployment_frequency)
z_scores_cvss = z_score(CVSS)

# Get indices of outliers for both datasets
outlier_indices_deployment = np.where(np.abs(z_scores_deployment) > threshold)
outlier_indices_cvss = np.where(np.abs(z_scores_cvss) > threshold)

# Combine outlier indices from both datasets
combined_outlier_indices = np.union1d(outlier_indices_deployment, outlier_indices_cvss)
x_values = [day for i, day in enumerate(deployment_frequency) if i not in combined_outlier_indices]

# Remove outliers from both datasets
cleaned_deployment_frequency_EWMA = [deployment_frequency[i] for i in range(len(deployment_frequency)) if i not in combined_outlier_indices]
cleaned_CVSS_EWMA = [CVSS[i] for i in range(len(CVSS)) if i not in combined_outlier_indices]
cleaned_deployment_frequency_linear = cleaned_deployment_frequency_EWMA
cleaned_CVSS_linear = cleaned_CVSS_EWMA
cleaned_deployment_frequency_polynomial = cleaned_deployment_frequency_EWMA
cleaned_CVSS_polynomial  = cleaned_CVSS_EWMA
# Calculate weights for WMA
weights_deployment = calculate_ewma_weights(alpha, len(cleaned_deployment_frequency_EWMA))
weights_cvss = calculate_ewma_weights(alpha, len(cleaned_CVSS_EWMA))

# Apply WMA to both datasets
smoothed_deployment_frequency_wma = wma(cleaned_deployment_frequency_EWMA, weights_deployment)
smoothed_CVSS_wma = wma(cleaned_CVSS_EWMA, weights_cvss)

# Calculate the residuals and SSR for both datasets
residuals_deployment = cleaned_deployment_frequency_EWMA[len(weights_deployment)-1:] - smoothed_deployment_frequency_wma
residuals_cvss = cleaned_CVSS_EWMA[len(weights_cvss)-1:] - smoothed_CVSS_wma

SSR_deployment = np.sum(residuals_deployment**2)
SSR_cvss = np.sum(residuals_cvss**2)

# Forecast the next three points for both datasets
forecasted_deployment_frequency = forecast_wma(cleaned_deployment_frequency_EWMA, weights_deployment, num_forecast)
forecasted_cvss = forecast_wma(cleaned_CVSS_EWMA, weights_cvss, num_forecast)


# ------------------linear--------------------
# Using numpy to perform linear regression
coefficients_linear_frequency = np.polyfit(x_values, cleaned_deployment_frequency_linear, 1)
poly_linear_frequency = np.poly1d(coefficients_linear_frequency)

coefficients_linear = np.polyfit(cleaned_deployment_frequency_linear, cleaned_CVSS_linear, 1)
poly_linear = np.poly1d(coefficients_linear)

# Calculate the residuals for linear regression
residuals_linear = cleaned_CVSS_linear - poly_linear(cleaned_deployment_frequency_linear)

# Calculate the sum of squared residuals (SSR) for linear regression
SSR_linear = np.sum(residuals_linear**2)

# Take the natural logarithm of SSR for linear regression
log_SSR_linear = np.log(SSR_linear)

# ------------------polynomial--------------------
# Using numpy to perform polynomial regression (degree = 2)
coefficients_poly_frequency = np.polyfit(x_values, cleaned_deployment_frequency_polynomial, 2)
poly_poly_polynomial = np.poly1d(coefficients_poly_frequency)

coefficients_poly = np.polyfit(cleaned_deployment_frequency_polynomial, cleaned_CVSS_polynomial, 2)
poly_poly = np.poly1d(coefficients_poly)

# Calculate the residuals for polynomial regression
residuals_poly = cleaned_CVSS_linear - poly_poly(cleaned_deployment_frequency_polynomial)

# Calculate the sum of squared residuals (SSR) for polynomial regression
SSR_poly = np.sum(residuals_poly**2)

# Take the natural logarithm of SSR for polynomial regression
log_SSR_poly = np.log(SSR_poly)
poly = None


# # Plotting the data points in the original order
# for i in range(len(cleaned_deployment_frequency_polynomial)):
#     plt.scatter(cleaned_deployment_frequency_polynomial[i], cleaned_CVSS_linear[i], color='blue', label='Data Points' if i == 0 else '')

# ------------------linear regression--------------------
plt.figure()  # Create a new figure for linear regression

# Plotting the data points in the original order
plt.scatter(cleaned_deployment_frequency_linear, cleaned_CVSS_linear, color='blue', label='Data Points')

# Plotting the regression line
# Plotting the regression line
# Plotting the regression line
largest_value = max(x_values)

# Calculate the next three days
next_days = np.array([largest_value + 1, largest_value + 2, largest_value + 3])
predicted_frequency = poly_linear_frequency(next_days)
predicted_frequency[predicted_frequency < 0] = 0

predicted_CVSS = poly_linear(predicted_frequency)
predicted_CVSS[predicted_CVSS < 0] = 0

plt.plot(cleaned_deployment_frequency_linear, poly_linear(cleaned_deployment_frequency_linear), color='green', label='Linear Regression Line')

# Plotting the predicted points
plt.scatter(predicted_frequency, predicted_CVSS, color='red', label='Predicted CVSS')

# Adding labels and legend
plt.xlabel('Deployment Frequency')
plt.ylabel('CVSS')
plt.legend()
plt.title('CVSS Prediction (linear)')
max_value = max(cleaned_CVSS_linear)
max_value_predict = max(predicted_CVSS)
max_value = max(max_value, max_value_predict)
upper_limit = max_value + 10
plt.ylim(0, upper_limit)

# Saving the plot as an image
plt.savefig('CVSSvsDeployment_linear.png')

# ------------------polynomial regression--------------------
plt.figure()  # Create a new figure for polynomial regression

# Plotting the data points in the original order
plt.scatter(cleaned_deployment_frequency_polynomial, cleaned_CVSS_polynomial, color='blue', label='Data Points')

# Plotting the regression line
largest_value = max(x_values)

# Calculate the next three days
next_days = np.array([largest_value + 1, largest_value + 2, largest_value + 3])
predicted_frequency = poly_poly_polynomial(next_days)
predicted_frequency[predicted_frequency < 0] = 0
predicted_CVSS = poly_poly(predicted_frequency)
predicted_CVSS[predicted_CVSS < 0] = 0

# Line added 24/10: create linspace from min to max of x axis
deploy_linspace = np.linspace(np.min(cleaned_deployment_frequency_polynomial), np.max(cleaned_deployment_frequency_polynomial), 50)
#plt.plot(cleaned_deployment_frequency_polynomial, poly_poly(cleaned_deployment_frequency_polynomial), color='green', label='Polynomial Regression Line')
# Line replaced 24/10: plot linspace against poly of linspace
plt.plot(deploy_linspace, poly_poly(deploy_linspace), color='green', label='Polynomial Regression Line')

# Plotting the predicted points
plt.scatter(predicted_frequency, predicted_CVSS, color='red', label='Predicted CVSS')

# Adding labels and legend
plt.xlabel('Deployment Frequency')
plt.ylabel('CVSS')
plt.legend()
plt.title('CVSS Prediction (polynomial)')
max_value = max(cleaned_deployment_frequency_polynomial)
max_value_predict = max(predicted_CVSS)
max_value = max(max_value, max_value_predict)
upper_limit = max_value + 10
plt.ylim(0, upper_limit)
# Saving the plot as an image
plt.savefig('CVSSvsDeployment_polynomial.png')


# ------------------weighted moving average--------------------
plt.figure()  # Create a new figure for weighted moving average

# Plotting the data points in the original order
print(cleaned_deployment_frequency_EWMA)
print(cleaned_CVSS_EWMA)
plt.scatter(cleaned_deployment_frequency_EWMA, cleaned_CVSS_EWMA, color='blue', label='Data Points')

# Plotting the WMA smoothing
plt.plot(cleaned_deployment_frequency_EWMA, smoothed_CVSS_wma, color='green', label='WMA Smoothing')

# Adding forecasted points to the plot
plt.scatter(forecasted_deployment_frequency, forecasted_cvss, color='red', label='Predicted CVSS', marker='o')

# Adding labels and legend
plt.xlabel('Deployment Frequency')
plt.ylabel('CVSS')
plt.legend()
plt.title('CVSS Prediction (EWMA)')
max_value = max(cleaned_CVSS_EWMA)
max_value_predict = max(forecasted_cvss)
max_value = max(max_value, max_value_predict)
upper_limit = max_value + 10
plt.ylim(0, upper_limit)

# Saving the plot as an image
plt.savefig('CVSSvsDeployment_wma.png')

# -----------------------use CVSS only-----------------------------
# Calculate mean and variance
# mean_cvss = np.mean(CVSS)
# variance_cvss = np.var(CVSS)

# # Calculate the absolute differences from the mean
# abs_diff_from_mean = np.abs(np.array(CVSS) - mean_cvss)

# # Get the indices of the top 10% data points with largest variance
# top_10_percent = math.ceil(len(CVSS) * 0.1)
# indices_to_remove = np.argsort(abs_diff_from_mean)[-top_10_percent:]
# print(top_10_percent)
# print(indices_to_remove)
# Remove the top 10% data points
# for EWMA
# cleaned_deployment_frequency_EWMA = [deployment_frequency[i] for i in range(len(deployment_frequency)) if i not in indices_to_remove]
# cleaned_CVSS_EWMA = [CVSS[i] for i in range(len(CVSS)) if i not in indices_to_remove]
# # for polynomial
# cleaned_deployment_frequency_polynomial = cleaned_deployment_frequency_EWMA
# cleaned_CVSS_polynomial = cleaned_CVSS_EWMA
# # for linear
# cleaned_deployment_frequency_linear = cleaned_deployment_frequency_EWMA
# cleaned_CVSS_linear = cleaned_CVSS_EWMA
# print("Length of cleaned_deployment_frequency:", len(CVSS))
# print("Length of cleaned_CVSS:", len(deployment_frequency))
# -------------------------------end of using CVSS only -----------------------------------


# --------------choose the best method according to the variance-----------------------
# # Choose the regression method with the smaller variance
# if variance_wma < min(SSR_linear, SSR_poly):
#     chosen_method = "Weighted Moving Average"
#     poly = None  # No regression line in this case
# else:
#     if log_SSR_linear < log_SSR_poly:
#         poly = poly_linear
#         chosen_method = "Linear"
#     else:
#         poly = poly_poly
#         chosen_method = "Polynomial"

# Print the chosen method
# print(f"The chosen method is: {chosen_method}")

# # Plotting the data points in the original order
# for i in range(len(cleaned_deployment_frequency)):
#     plt.scatter(cleaned_deployment_frequency[i], CVSS[i], color='blue', label='Data Points' if i == 0 else '')

# # Plotting the regression line if applicable
# if poly is not None and chosen_method != "Weighted Moving Average":
#     next_days = np.array([15, 16, 17])
#     predicted_CVSS = poly(next_days)
    
#     # Plotting the regression line in red
#     plt.plot(cleaned_deployment_frequency, poly(cleaned_deployment_frequency), color='green', label=f'{chosen_method} Regression Line')
    
#     # Plotting the predicted points in red
#     plt.scatter(next_days, predicted_CVSS, color='red', label=f'Predicted CVSS ({chosen_method})')

# print(len(cleaned_deployment_frequency))
# print(len(smoothed_CVSS_wma))

# # Plotting the predicted CVSS for WMA
# if chosen_method == "Weighted Moving Average":
#     plt.plot(cleaned_deployment_frequency, smoothed_CVSS_wma, color='green', label=f'WMA Smoothing')

#     # Add forecasted points to the plot
#     forecasted_x = np.arange(len(cleaned_deployment_frequency), len(cleaned_deployment_frequency) + num_forecast)
#     plt.plot(forecasted_x, forecasted_points, color='red', label='Forecasted Points')

# # Adding labels and legend
# plt.xlabel('Deployment Frequency')
# plt.ylabel('CVSS')
# plt.legend()

# # Saving the updated plot as an image
# plt.savefig('CVSSvsDeployment.png')
# -------------------------end ------------------------------